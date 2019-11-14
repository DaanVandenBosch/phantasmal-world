import { Instruction, InstructionSegment, Segment, SegmentType, AsmToken } from "../instructions";
import {
    OP_ADD,
    OP_ADDI,
    OP_AND,
    OP_ANDI,
    OP_ARG_PUSHB,
    OP_ARG_PUSHL,
    OP_ARG_PUSHR,
    OP_ARG_PUSHW,
    OP_ARG_PUSHA,
    OP_ARG_PUSHS,
    OP_CALL,
    OP_CLEAR,
    OP_DIV,
    OP_DIVI,
    OP_EXIT,
    OP_FADD,
    OP_FADDI,
    OP_FDIV,
    OP_FDIVI,
    OP_FMUL,
    OP_FMULI,
    OP_FSUB,
    OP_FSUBI,
    OP_JMP,
    OP_LET,
    OP_LETB,
    OP_LETI,
    OP_LETW,
    OP_MOD,
    OP_MODI,
    OP_MUL,
    OP_MULI,
    OP_NOP,
    OP_OR,
    OP_ORI,
    OP_RET,
    OP_REV,
    OP_SET,
    OP_SHIFT_LEFT,
    OP_SHIFT_RIGHT,
    OP_SUB,
    OP_SUBI,
    OP_SYNC,
    OP_THREAD,
    OP_XOR,
    OP_XORI,
    OP_JMP_E,
    OP_JMPI_E,
    OP_JMP_ON,
    OP_JMP_OFF,
    OP_JMP_NE,
    OP_JMPI_NE,
    OP_UJMP_G,
    OP_UJMPI_G,
    OP_JMP_G,
    OP_JMPI_G,
    OP_UJMP_L,
    OP_UJMPI_L,
    OP_JMP_L,
    OP_JMPI_L,
    OP_UJMP_GE,
    OP_UJMPI_GE,
    OP_JMP_GE,
    OP_JMPI_GE,
    OP_UJMP_LE,
    OP_UJMPI_LE,
    OP_JMP_LE,
    OP_JMPI_LE,
    OP_STACK_POP,
    OP_STACK_PUSH,
    OP_STACK_PUSHM,
    OP_STACK_POPM,
    Kind,
    OP_WINDOW_MSG,
    OP_ADD_MSG,
    OP_WINEND,
    OP_LETA,
    OP_FLET,
    OP_FLETI,
    OP_GET_RANDOM,
    OP_GETTIME,
    OP_SET_EPISODE,
    OP_LIST,
} from "../opcodes";
import { VirtualMachineMemoryBuffer, VirtualMachineMemory } from "./memory";
import {
    ComparisonOperation,
    numeric_ops,
    comparison_ops,
    rest,
    BinaryNumericOperation,
    andsecond,
    andreduce,
} from "./utils";
import { VirtualMachineIO } from "./io";
import { VMIOStub } from "./VMIOStub";
import { rand, srand, GetTickCount } from "./windows";
import { QuestModel } from "../../model/QuestModel";
import { convert_quest_from_model } from "../../stores/model_conversion";
import { Episode } from "../../../core/data_formats/parsing/quest/Episode";

const REGISTERS_BASE_ADDRESS = 0x00a954b0;
const REGISTER_COUNT = 256;
const REGISTER_SIZE = 4;
const VARIABLE_STACK_LENGTH = 16; // TODO: verify this value
const ARG_STACK_SLOT_SIZE = 4;
const ARG_STACK_LENGTH = 8;
const STRING_ARG_STORE_ADDRESS = 0x00a92700;
const STRING_ARG_STORE_SIZE = 1024; // TODO: verify this value
const FLOAT_EPSILON = 1.19e-7;
const ENTRY_SEGMENT = 0;
const LIST_ITEM_DELIMITER = "\n";

export enum ExecutionResult {
    Ok,
    WaitingVsync,
    Halted,
    /**
     * Waiting for any keypress. No method call required.
     */
    WaitingInput,
    /**
     * Waiting for a value to be selected in a list.
     * Call `list_select` to set selection.
     */
    WaitingSelection,
}

function encode_episode_number(ep: Episode): number {
    switch (ep) {
        case Episode.I:
            return 0;
        case Episode.II:
            return 1;
        case Episode.IV:
            return 2;
    }
}

export class VirtualMachine {
    private memory = new VirtualMachineMemory();
    private registers = this.memory.allocate(
        REGISTER_SIZE * REGISTER_COUNT,
        REGISTERS_BASE_ADDRESS,
    )!;
    private string_arg_store = this.memory.allocate(
        STRING_ARG_STORE_SIZE,
        STRING_ARG_STORE_ADDRESS,
    );
    private quest?: QuestModel;
    private object_code: readonly Segment[] = [];
    private label_to_seg_idx: Map<number, number> = new Map();
    private thread: Thread[] = [];
    private thread_idx = 0;
    private window_msg_open = false;
    private set_episode_called = false;
    private list_open = false;
    private selection_reg = 0;
    private cur_srcloc?: AsmToken;

    constructor(private io: VirtualMachineIO = new VMIOStub()) {
        srand(GetTickCount());
    }

    /**
     * Halts and resets the VM, then loads new quest.
     */
    load_quest(quest_model: QuestModel): void {
        const quest = convert_quest_from_model(quest_model);
        this.load_object_code(quest.object_code);
    }

    /**
     * Halts and resets the VM, then loads new object code.
     * Opcodes which use quest data outside the object code
     * will not work if calling this method directly.
     * Use {@link VirtualMachine.load_quest} if full functionality is needed.
     */
    load_object_code(object_code: readonly Segment[]): void {
        this.halt();
        this.quest = undefined;
        this.registers.zero();
        this.string_arg_store.zero();
        this.object_code = object_code;
        this.label_to_seg_idx.clear();
        this.set_episode_called = false;
        this.list_open = false;
        this.selection_reg = 0;
        this.cur_srcloc = undefined;

        let i = 0;

        for (const segment of this.object_code) {
            for (const label of segment.labels) {
                this.label_to_seg_idx.set(label, i);
            }

            i++;
        }
    }

    /**
     * Schedules concurrent execution of the code at the given label.
     */
    start_thread(label: number): void {
        const seg_idx = this.label_to_seg_idx.get(label);
        const segment = seg_idx == undefined ? undefined : this.object_code[seg_idx];

        if (segment == undefined) {
            throw new Error(`Unknown label ${label}.`);
        }

        if (segment.type !== SegmentType.Instructions) {
            throw new Error(
                `Label ${label} points to a ${SegmentType[segment.type]} segment, expecting ${
                    SegmentType[SegmentType.Instructions]
                }.`,
            );
        }

        this.thread.push(
            new Thread(
                this.io,
                new ExecutionLocation(seg_idx!, 0),
                this.memory.allocate(ARG_STACK_SLOT_SIZE * ARG_STACK_LENGTH),
                true,
            ),
        );
    }

    private dispose_thread(thread_idx: number): void {
        this.thread[thread_idx].dispose();
        this.thread.splice(thread_idx, 1);
    }

    /**
     * Executes the next instruction if one is scheduled.
     */
    execute(): ExecutionResult {
        let srcloc: AsmToken | undefined;

        if (this.thread.length === 0) {
            this.cur_srcloc = undefined;
            return ExecutionResult.Halted
        }

        try {
            const exec = this.thread[this.thread_idx];
            const inst = this.get_next_instruction_from_thread(exec);

            if (inst.asm && inst.asm.mnemonic) {
                srcloc = inst.asm.mnemonic;
            }

            this.cur_srcloc = srcloc;

            return this.execute_instruction(exec, inst, srcloc);
        } catch (err) {
            if (!(err instanceof Error)) {
                err = new Error(String(err));
            }

            this.halt();
            this.io.error(err, srcloc);

            return ExecutionResult.Halted;
        }
    }

    private execute_instruction(
        exec: Thread,
        inst: Instruction,
        srcloc?: AsmToken,
    ): ExecutionResult {
        if (this.thread_idx >= this.thread.length) return ExecutionResult.WaitingVsync;

        let result = ExecutionResult.Ok;

        const arg_vals = inst.args.map(arg => arg.value);
        // eslint-disable-next-line
        const [arg0, arg1, arg2, arg3, arg4, arg5, arg6, arg7] = arg_vals;

        // helper for conditional jump opcodes
        const conditional_jump_args: (
            cond: ComparisonOperation,
        ) => [Thread, number, ComparisonOperation, number, number] = cond => [
            exec,
            arg2,
            cond,
            arg0,
            arg1,
        ];

        // previous instruction must've been `list`.
        // list may not exist after the instruction
        if (this.list_open) {
            this.list_open = false;
        }

        switch (inst.opcode.code) {
            case OP_NOP.code:
                break;
            case OP_RET.code:
                this.pop_call_stack(this.thread_idx, exec);
                break;
            case OP_SYNC.code:
                this.thread_idx++;
                break;
            case OP_EXIT.code:
                this.halt();
                break;
            case OP_THREAD.code:
                this.start_thread(arg0);
                break;
            // integer lets
            case OP_LET.code:
                this.set_register_signed(arg0, this.get_register_signed(arg1));
                break;
            case OP_LETI.code:
                this.set_register_signed(arg0, arg1);
                break;
            case OP_LETB.code:
                this.set_register_byte(arg0, arg1);
                break;
            case OP_LETW.code:
                this.set_register_word(arg0, arg1);
                break;
            case OP_LETA.code:
                this.set_register_unsigned(arg0, this.get_register_address(arg0));
                break;
            // float lets
            case OP_FLET.code:
                this.set_register_float(arg0, this.get_register_float(arg1));
                break;
            case OP_FLETI.code:
                this.set_register_float(arg0, arg1);
                break;
            case OP_SET.code:
                this.set_register_signed(arg0, 1);
                break;
            case OP_CLEAR.code:
                this.set_register_signed(arg0, 0);
                break;
            case OP_REV.code:
                this.set_register_signed(arg0, this.get_register_signed(arg0) === 0 ? 1 : 0);
                break;
            case OP_CALL.code:
                this.push_call_stack(exec, arg0);
                break;
            case OP_JMP.code:
                this.jump_to_label(exec, arg0);
                break;
            case OP_ARG_PUSHR.code:
                // deref given register ref
                exec.push_arg(this.get_register_signed(arg0), Kind.DWord);
                break;
            case OP_ARG_PUSHL.code:
                exec.push_arg(inst.args[0].value, Kind.DWord);
                break;
            case OP_ARG_PUSHB.code:
                exec.push_arg(inst.args[0].value, Kind.Byte);
                break;
            case OP_ARG_PUSHW.code:
                exec.push_arg(inst.args[0].value, Kind.Word);
                break;
            case OP_ARG_PUSHA.code:
                // push address of register
                exec.push_arg(this.get_register_address(inst.args[0].value), Kind.DWord);
                break;
            case OP_ARG_PUSHS.code:
                if (typeof arg0 === "string") {
                    // process tags
                    const string_arg = this.parse_template_string(arg0);
                    // store string and push its address
                    this.string_arg_store.write_string_utf16_at(
                        0,
                        string_arg,
                        string_arg.length * 2 + 2,
                    );
                    exec.push_arg(this.string_arg_store.address, Kind.String);
                }
                break;
            // integer arithmetic operations
            case OP_ADD.code:
                this.do_integer_op_with_register(arg0, arg1, numeric_ops.add);
                break;
            case OP_ADDI.code:
                this.do_integer_op_with_literal(arg0, arg1, numeric_ops.add);
                break;
            case OP_SUB.code:
                this.do_integer_op_with_register(arg0, arg1, numeric_ops.sub);
                break;
            case OP_SUBI.code:
                this.do_integer_op_with_literal(arg0, arg1, numeric_ops.sub);
                break;
            case OP_MUL.code:
                this.do_integer_op_with_register(arg0, arg1, numeric_ops.mul);
                break;
            case OP_MULI.code:
                this.do_integer_op_with_literal(arg0, arg1, numeric_ops.mul);
                break;
            case OP_DIV.code:
                this.do_integer_op_with_register(arg0, arg1, numeric_ops.idiv);
                break;
            case OP_DIVI.code:
                this.do_integer_op_with_literal(arg0, arg1, numeric_ops.idiv);
                break;
            case OP_MOD.code:
                this.do_integer_op_with_register(arg0, arg1, numeric_ops.mod);
                break;
            case OP_MODI.code:
                this.do_integer_op_with_literal(arg0, arg1, numeric_ops.mod);
                break;
            // float arithmetic operations
            case OP_FADD.code:
                this.do_float_op_with_register(arg0, arg1, numeric_ops.add);
                break;
            case OP_FADDI.code:
                this.do_float_op_with_literal(arg0, Math.fround(arg1), numeric_ops.add);
                break;
            case OP_FSUB.code:
                this.do_float_op_with_register(arg0, arg1, numeric_ops.sub);
                break;
            case OP_FSUBI.code:
                this.do_float_op_with_literal(arg0, Math.fround(arg1), numeric_ops.sub);
                break;
            case OP_FMUL.code:
                this.do_float_op_with_register(arg0, arg1, numeric_ops.mul);
                break;
            case OP_FMULI.code:
                this.do_float_op_with_literal(arg0, Math.fround(arg1), numeric_ops.mul);
                break;
            case OP_FDIV.code:
                this.do_float_op_with_register(arg0, arg1, numeric_ops.div);
                break;
            case OP_FDIVI.code:
                this.do_float_op_with_literal(arg0, Math.fround(arg1), numeric_ops.div);
                break;
            // bit operations
            case OP_AND.code:
                this.do_integer_op_with_register(arg0, arg1, numeric_ops.and);
                break;
            case OP_ANDI.code:
                this.do_integer_op_with_literal(arg0, arg1, numeric_ops.and);
                break;
            case OP_OR.code:
                this.do_integer_op_with_register(arg0, arg1, numeric_ops.or);
                break;
            case OP_ORI.code:
                this.do_integer_op_with_literal(arg0, arg1, numeric_ops.or);
                break;
            case OP_XOR.code:
                this.do_integer_op_with_register(arg0, arg1, numeric_ops.xor);
                break;
            case OP_XORI.code:
                this.do_integer_op_with_literal(arg0, arg1, numeric_ops.xor);
                break;
            // shift operations
            case OP_SHIFT_LEFT.code:
                this.do_integer_op_with_register(arg0, arg1, numeric_ops.shl);
                break;
            case OP_SHIFT_RIGHT.code:
                this.do_integer_op_with_register(arg0, arg1, numeric_ops.shr);
                break;
            // conditional jumps
            case OP_JMP_ON.code:
                // all eq 1?
                this.conditional_jump(
                    exec,
                    arg0,
                    comparison_ops.eq,
                    1,
                    ...rest(arg_vals).map(reg => this.get_register_signed(reg)),
                );
                break;
            case OP_JMP_OFF.code:
                // all eq 0?
                this.conditional_jump(
                    exec,
                    arg0,
                    comparison_ops.eq,
                    0,
                    ...rest(arg_vals).map(reg => this.get_register_signed(reg)),
                );
                break;
            case OP_JMP_E.code:
                this.signed_conditional_jump_with_register(
                    ...conditional_jump_args(comparison_ops.eq),
                );
                break;
            case OP_JMPI_E.code:
                this.signed_conditional_jump_with_literal(
                    ...conditional_jump_args(comparison_ops.eq),
                );
                break;
            case OP_JMP_NE.code:
                this.signed_conditional_jump_with_register(
                    ...conditional_jump_args(comparison_ops.neq),
                );
                break;
            case OP_JMPI_NE.code:
                this.signed_conditional_jump_with_literal(
                    ...conditional_jump_args(comparison_ops.neq),
                );
                break;
            case OP_UJMP_G.code:
                this.unsigned_conditional_jump_with_register(
                    ...conditional_jump_args(comparison_ops.gt),
                );
                break;
            case OP_UJMPI_G.code:
                this.unsigned_conditional_jump_with_literal(
                    ...conditional_jump_args(comparison_ops.gt),
                );
                break;
            case OP_JMP_G.code:
                this.signed_conditional_jump_with_register(
                    ...conditional_jump_args(comparison_ops.gt),
                );
                break;
            case OP_JMPI_G.code:
                this.signed_conditional_jump_with_literal(
                    ...conditional_jump_args(comparison_ops.gt),
                );
                break;
            case OP_UJMP_L.code:
                this.unsigned_conditional_jump_with_register(
                    ...conditional_jump_args(comparison_ops.lt),
                );
                break;
            case OP_UJMPI_L.code:
                this.unsigned_conditional_jump_with_literal(
                    ...conditional_jump_args(comparison_ops.lt),
                );
                break;
            case OP_JMP_L.code:
                this.signed_conditional_jump_with_register(
                    ...conditional_jump_args(comparison_ops.lt),
                );
                break;
            case OP_JMPI_L.code:
                this.signed_conditional_jump_with_literal(
                    ...conditional_jump_args(comparison_ops.lt),
                );
                break;
            case OP_UJMP_GE.code:
                this.unsigned_conditional_jump_with_register(
                    ...conditional_jump_args(comparison_ops.gte),
                );
                break;
            case OP_UJMPI_GE.code:
                this.unsigned_conditional_jump_with_literal(
                    ...conditional_jump_args(comparison_ops.gte),
                );
                break;
            case OP_JMP_GE.code:
                this.signed_conditional_jump_with_register(
                    ...conditional_jump_args(comparison_ops.gte),
                );
                break;
            case OP_JMPI_GE.code:
                this.signed_conditional_jump_with_literal(
                    ...conditional_jump_args(comparison_ops.gte),
                );
                break;
            case OP_UJMP_LE.code:
                this.unsigned_conditional_jump_with_register(
                    ...conditional_jump_args(comparison_ops.lte),
                );
                break;
            case OP_UJMPI_LE.code:
                this.unsigned_conditional_jump_with_literal(
                    ...conditional_jump_args(comparison_ops.lte),
                );
                break;
            case OP_JMP_LE.code:
                this.signed_conditional_jump_with_register(
                    ...conditional_jump_args(comparison_ops.lte),
                );
                break;
            case OP_JMPI_LE.code:
                this.signed_conditional_jump_with_literal(
                    ...conditional_jump_args(comparison_ops.lte),
                );
                break;
            // variable stack operations
            case OP_STACK_PUSH.code:
                this.push_variable_stack(exec, arg0, 1);
                break;
            case OP_STACK_POP.code:
                this.pop_variable_stack(exec, arg0, 1);
                break;
            case OP_STACK_PUSHM.code:
                this.push_variable_stack(exec, arg0, arg1);
                break;
            case OP_STACK_POPM.code:
                this.pop_variable_stack(exec, arg0, arg1);
                break;
            case OP_LIST.code:
                if (!this.window_msg_open) {
                    const args = exec.fetch_args(inst);
                    const list_items = this.deref_string(args[1]).split(LIST_ITEM_DELIMITER);

                    result = ExecutionResult.WaitingSelection;
                    this.list_open = true;
                    this.selection_reg = args[0];
                    this.io.list(list_items);
                }
                break;
            case OP_WINDOW_MSG.code:
                if (!this.window_msg_open) {
                    const args = exec.fetch_args(inst);
                    const str = this.deref_string(args[0]);

                    result = ExecutionResult.WaitingInput;
                    this.window_msg_open = true;
                    this.io.window_msg(str);
                }
                break;
            case OP_ADD_MSG.code:
                if (this.window_msg_open) {
                    const args = exec.fetch_args(inst);
                    const str = this.deref_string(args[0]);

                    result = ExecutionResult.WaitingInput;
                    this.io.add_msg(str);
                }
                break;
            case OP_GETTIME.code:
                this.set_register_unsigned(arg0, Math.floor(Date.now() / 1000));
                break;
            case OP_WINEND.code:
                if (this.window_msg_open) {
                    this.window_msg_open = false;
                    this.io.winend();
                }
                break;
            case OP_GET_RANDOM.code:
                {
                    const low = this.get_register_signed(arg0);
                    const hi = this.get_register_signed(arg0 + 1);

                    const r = rand();
                    let result = Math.floor(Math.fround(r / 32768.0) * hi);

                    // intentional. this is how the game does it.
                    if (low >= result) {
                        result = low;
                    }

                    this.set_register_signed(arg1, result);
                }
                break;
            case OP_SET_EPISODE.code:
                if (this.set_episode_called) {
                    this.io.warning("Calling set_episode more than once is not supported.", srcloc);
                    break;
                }

                this.set_episode_called = true;

                if (this.get_current_execution_location().seg_idx !== ENTRY_SEGMENT) {
                    this.io.warning(
                        `Calling set_episode outside of segment ${ENTRY_SEGMENT} is not supported.`,
                        srcloc,
                    );
                    break;
                }

                if (!this.quest) {
                    this.missing_quest_data_warning(OP_SET_EPISODE.mnemonic, srcloc);
                    break;
                }

                if (encode_episode_number(this.quest.episode) !== arg0) {
                    this.io.warning(
                        "Calling set_episode with an argument that does not" +
                            "match the quest's designated episode is not supported.",
                        srcloc,
                    );
                    break;
                }

                break;
            default:
                this.io.warning(`Unsupported instruction: ${inst.opcode.mnemonic}.`);
                break;
        }

        // advance instruction "pointer"
        if (exec.call_stack.length) {
            const top = exec.call_stack_top();
            const segment = this.object_code[top.seg_idx] as InstructionSegment;

            // move to next instruction
            if (++top.inst_idx >= segment.instructions.length) {
                // segment ended, move to next segment
                if (++top.seg_idx >= this.object_code.length) {
                    // eof
                    this.dispose_thread(this.thread_idx);
                } else {
                    top.inst_idx = 0;
                }
            }
        }

        return result;
    }

    /**
     * Signal to the VM that a vsync has happened.
     */
    vsync(): void {
        if (this.thread_idx >= this.thread.length) {
            this.thread_idx = 0;
        }
    }

    /**
     * Halts execution of all threads.
     */
    halt(): void {
        for (const thread of this.thread) {
            thread.dispose();
        }

        this.window_msg_open = false;

        this.thread = [];
        this.thread_idx = 0;
    }

    public get_register_signed(reg: number): number {
        return this.registers.i32_at(REGISTER_SIZE * reg);
    }

    private set_register_signed(reg: number, value: number): void {
        this.registers.write_i32_at(REGISTER_SIZE * reg, value);
    }

    public get_register_unsigned(reg: number): number {
        return this.registers.u32_at(REGISTER_SIZE * reg);
    }

    private set_register_unsigned(reg: number, value: number): void {
        this.registers.write_u32_at(REGISTER_SIZE * reg, value);
    }

    public get_register_word(reg: number): number {
        return this.registers.u16_at(REGISTER_SIZE * reg);
    }

    private set_register_word(reg: number, value: number): void {
        this.registers.write_u16_at(REGISTER_SIZE * reg, value);
    }

    public get_register_byte(reg: number): number {
        return this.registers.u8_at(REGISTER_SIZE * reg);
    }

    public set_register_byte(reg: number, value: number): void {
        this.registers.write_u8_at(REGISTER_SIZE * reg, value);
    }

    public get_register_float(reg: number): number {
        return this.registers.f32_at(REGISTER_SIZE * reg);
    }

    private set_register_float(reg: number, value: number): void {
        this.registers.write_f32_at(REGISTER_SIZE * reg, value);
    }

    private do_integer_op_with_register(
        reg1: number,
        reg2: number,
        op: BinaryNumericOperation,
    ): void {
        this.do_integer_op_with_literal(reg1, this.get_register_signed(reg2), op);
    }

    private do_integer_op_with_literal(
        reg: number,
        literal: number,
        op: BinaryNumericOperation,
    ): void {
        if ((op === numeric_ops.div || op === numeric_ops.idiv) && literal === 0) {
            throw new Error("Division by zero");
        }
        this.set_register_signed(reg, op(this.get_register_signed(reg), literal));
    }

    private do_float_op_with_register(
        reg1: number,
        reg2: number,
        op: BinaryNumericOperation,
    ): void {
        this.do_float_op_with_literal(reg1, this.get_register_float(reg2), op);
    }

    private do_float_op_with_literal(
        reg: number,
        literal: number,
        op: BinaryNumericOperation,
    ): void {
        if ((op === numeric_ops.div || op === numeric_ops.idiv) && literal === 0) {
            throw new Error("Division by zero");
        }
        this.set_register_float(reg, op(this.get_register_float(reg), literal));
    }

    private push_call_stack(exec: Thread, label: number): void {
        const seg_idx = this.label_to_seg_idx.get(label);

        if (seg_idx == undefined) {
            throw new Error(`Invalid label called: ${label}.`);
        } else {
            const segment = this.object_code[seg_idx];

            if (segment.type !== SegmentType.Instructions) {
                throw new Error(
                    `Label ${label} points to a ${SegmentType[segment.type]} segment, expecting ${
                        SegmentType[SegmentType.Instructions]
                    }.`,
                );
            } else {
                exec.call_stack.push(new ExecutionLocation(seg_idx, -1));
            }
        }
    }

    private pop_call_stack(idx: number, exec: Thread): void {
        exec.call_stack.pop();

        if (exec.call_stack.length >= 1) {
            const top = exec.call_stack_top();
            const segment = this.object_code[top.seg_idx];

            if (!segment || segment.type !== SegmentType.Instructions) {
                throw new Error(`Invalid segment index ${top.seg_idx}.`);
            }
        } else {
            // popped off the last return address
            // which means this is the end of the function this thread was started on
            // which means this is the end of this thread
            this.thread.splice(idx, 1);
        }
    }

    private jump_to_label(exec: Thread, label: number): void {
        const top = exec.call_stack_top();
        const seg_idx = this.label_to_seg_idx.get(label);

        if (seg_idx == undefined) {
            throw new Error(`Invalid jump label: ${label}.`);
        } else {
            top.seg_idx = seg_idx;
            top.inst_idx = -1;
        }
    }

    private signed_conditional_jump_with_register(
        exec: Thread,
        label: number,
        condition: ComparisonOperation,
        reg1: number,
        reg2: number,
    ): void {
        this.conditional_jump(
            exec,
            label,
            condition,
            this.get_register_signed(reg1),
            this.get_register_signed(reg2),
        );
    }

    private signed_conditional_jump_with_literal(
        exec: Thread,
        label: number,
        condition: ComparisonOperation,
        reg: number,
        literal: number,
    ): void {
        this.conditional_jump(exec, label, condition, this.get_register_signed(reg), literal);
    }

    private unsigned_conditional_jump_with_register(
        exec: Thread,
        label: number,
        condition: ComparisonOperation,
        reg1: number,
        reg2: number,
    ): void {
        this.conditional_jump(
            exec,
            label,
            condition,
            this.get_register_unsigned(reg1),
            this.get_register_unsigned(reg2),
        );
    }

    private unsigned_conditional_jump_with_literal(
        exec: Thread,
        label: number,
        condition: ComparisonOperation,
        reg: number,
        literal: number,
    ): void {
        this.conditional_jump(exec, label, condition, this.get_register_unsigned(reg), literal);
    }

    private conditional_jump(
        exec: Thread,
        label: number,
        condition: ComparisonOperation,
        ...vals: number[]
    ): void {
        const chain_cmp = andsecond.bind<
            null,
            ComparisonOperation,
            Parameters<ComparisonOperation>,
            any
        >(null, condition);
        if (andreduce(chain_cmp, vals) !== undefined) {
            this.jump_to_label(exec, label);
        }
    }

    private push_variable_stack(exec: Thread, base_reg: number, num_push: number): void {
        const end = base_reg + num_push;

        if (end > REGISTER_COUNT) {
            throw new Error("Variable stack: Invalid register");
        }

        if (exec.variable_stack.length + num_push > VARIABLE_STACK_LENGTH) {
            throw new Error("Variable stack: Stack overflow");
        }

        for (let r = base_reg; r < end; r++) {
            exec.variable_stack.push(this.get_register_unsigned(r));
        }
    }

    private pop_variable_stack(exec: Thread, base_reg: number, num_pop: number): void {
        const end = base_reg + num_pop;

        if (end > REGISTER_COUNT) {
            throw new Error("Variable stack: Invalid register");
        }

        if (exec.variable_stack.length < num_pop) {
            throw new Error("Variable stack: Stack underflow");
        }

        for (let r = end - 1; r >= base_reg; r--) {
            this.set_register_unsigned(r, exec.variable_stack.pop()!);
        }
    }

    private get_next_instruction_from_thread(exec: Thread): Instruction {
        if (exec.call_stack.length) {
            const top = exec.call_stack_top();
            const segment = this.object_code[top.seg_idx];

            if (!segment || segment.type !== SegmentType.Instructions) {
                throw new Error(`Invalid segment index ${top.seg_idx}.`);
            }

            const inst = segment.instructions[top.inst_idx];

            if (!inst) {
                throw new Error(
                    `Invalid instruction index ${top.inst_idx} for segment ${top.seg_idx}.`,
                );
            }

            return inst;
        } else {
            throw new Error(`Call stack is empty.`);
        }
    }

    private get_register_address(reg: number): number {
        return this.registers.address + reg * REGISTER_SIZE;
    }

    private deref_string(address: number): string {
        const slot = this.memory.get(address);

        let str: string = "";

        if (slot !== undefined) {
            str = slot.buffer.string_utf16_at(slot.byte_offset, Infinity, true);
        }

        return str;
    }

    public get_current_execution_location(): Readonly<ExecutionLocation> {
        return this.thread[this.thread_idx].call_stack_top();
    }

    private missing_quest_data_warning(info: string, srcloc?: AsmToken): void {
        this.io.warning(
            `Opcode execution failed because the VM was missing quest data: ${info}`,
            srcloc,
        );
    }

    /**
     * When the list opcode is used, call this method to select a value in the list.
     */
    public list_select(idx: number): void {
        if (!this.list_open) {
            throw new Error("list_select may not be called if there is no list open");
        }
        this.set_register_unsigned(this.selection_reg, idx);
    }

    private parse_template_string(template: string): string {
        const exact_tags: Record<string, string | (() => string)> = {
            // TODO: get real values for these
            "hero name": "PLACEHOLDER",
            "hero job": "PLACEHOLDER",
            "name hero": "PLACEHOLDER",
            "name job": "PLACEHOLDER",
            // intentionally hardcoded
            time: "01:12",
            // used in cmode
            "award item": "PLACEHOLDER",
            "challenge title": "PLACEHOLDER",
            // set by opcode get_pl_name
            pl_name: "PLACEHOLDER",
            pl_job: "PLACEHOLDER",
            last_word: "PLACEHOLDER",
            last_chat: "PLACEHOLDER",
            team_name: "PLACEHOLDER",
            // does not appear to be used in any sega quests
            meseta_slot_prize: "PLACEHOLDER",
        };
        const pattern_tags: [RegExp, (arg: string) => string][] = [
            [
                /^color ([0-9]+)$/,
                arg => {
                    // TODO: decide how to handle this
                    return `<color ${arg}>`;
                },
            ],
            [
                /^r([0-9]{1,3})$/,
                arg => {
                    const num = parseInt(arg);

                    if (isNaN(num)) {
                        return "";
                    }

                    return this.get_register_unsigned(num).toString();
                },
            ],
            [
                /^f([0-9]{1,3})$/,
                arg => {
                    const num = parseInt(arg);

                    if (isNaN(num)) {
                        return "";
                    }

                    return this.get_register_float(num).toFixed(6);
                },
            ],
        ];

        const tag_start_char = "<";
        const tag_end_char = ">";

        let tag_open = false;
        let tag_start_idx = -1;

        let i = 0;
        let len = template.length;
        // iterate through template
        while (i < len) {
            const char = template[i];

            // end of tag
            if (tag_open && char === tag_end_char) {
                tag_open = false;

                // extract key from tag
                const tag_end_idx = i;
                const key = template.slice(tag_start_idx + 1, tag_end_idx);

                // get value
                let val: string | (() => string) | undefined = undefined;

                // check if matches pattern
                for (const [pattern, handler] of pattern_tags) {
                    const match = pattern.exec(key);
                    if (match && match[1] !== undefined) {
                        val = handler(match[1]);
                        break;
                    }
                }

                // check if matches tag
                if (val === undefined) {
                    val = exact_tags[key];
                }

                // not a valid tag, replace with empty string
                if (val === undefined) {
                    val = "";
                }
                // run function and memoize result
                else if (typeof val === "function") {
                    const memo = val();
                    exact_tags[key] = memo;
                    val = memo;
                }

                // replace tag with value in template
                template = template.slice(0, tag_start_idx) + val + template.slice(tag_end_idx + 1);

                // adjust position
                const offset = val.length - (key.length + 2);
                i += offset;
                len += offset;
            }
            // mark start of tag
            else if (char === tag_start_char) {
                tag_open = true;
                tag_start_idx = i;
            }

            i++;
        }

        // remove open tag if it was not closed until the end
        if (tag_open) {
            template = template.slice(0, tag_start_idx);
        }

        return template;
    }

    public get_current_source_location(): AsmToken | undefined {
        return this.cur_srcloc;
    }
}

export class ExecutionLocation {
    constructor(public seg_idx: number, public inst_idx: number) {}
}

type ArgStackTypeList = [Kind, Kind, Kind, Kind, Kind, Kind, Kind, Kind];

class Thread {
    /**
     * Call stack. The top element describes the instruction about to be executed.
     */
    public call_stack: ExecutionLocation[] = [];

    private arg_stack: VirtualMachineMemoryBuffer;
    private arg_stack_counter: number = 0;
    private arg_stack_types: ArgStackTypeList = Array(ARG_STACK_LENGTH).fill(
        Kind.Any,
    ) as ArgStackTypeList;

    public variable_stack: number[] = [];
    /**
     * Global or floor-local?
     */
    public global: boolean;

    constructor(
        public io: VirtualMachineIO,
        next: ExecutionLocation,
        arg_stack: VirtualMachineMemoryBuffer,
        global: boolean,
    ) {
        this.call_stack = [next];
        this.global = global;

        this.arg_stack = arg_stack;
    }

    public call_stack_top(): ExecutionLocation {
        return this.call_stack[this.call_stack.length - 1];
    }

    public push_arg(data: number, type: Kind): void {
        if (this.arg_stack_counter >= ARG_STACK_LENGTH) {
            throw new Error("Argument stack: Stack overflow");
        }

        this.arg_stack.write_u32_at(this.arg_stack_counter * ARG_STACK_SLOT_SIZE, data);
        this.arg_stack_types[this.arg_stack_counter] = type;

        this.arg_stack_counter++;
    }

    public fetch_args(inst: Instruction): number[] {
        const args: number[] = [];
        const srcloc: AsmToken | undefined = inst.asm && inst.asm.mnemonic;

        if (inst.opcode.params.length !== this.arg_stack_counter) {
            this.io.warning("Argument stack: Argument count mismatch", srcloc);
        }

        for (let i = 0; i < inst.opcode.params.length; i++) {
            const param = inst.opcode.params[i];

            if (param.type.kind !== this.arg_stack_types[i]) {
                this.io.warning("Argument stack: Argument type mismatch", srcloc);
            }

            const arg_slot_offset = i * ARG_STACK_SLOT_SIZE;
            switch (param.type.kind) {
                case Kind.Byte:
                    args.push(this.arg_stack.u8_at(arg_slot_offset));
                    break;
                case Kind.Word:
                    args.push(this.arg_stack.u16_at(arg_slot_offset));
                    break;
                case Kind.DWord:
                case Kind.String:
                    args.push(this.arg_stack.u32_at(arg_slot_offset));
                    break;
                case Kind.RegTupRef:
                    if (param.type.register_tuples.length > 0) {
                        args.push(this.arg_stack.u8_at(arg_slot_offset));
                    }
                    break;
                default:
                    throw new Error(
                        `Argument stack: Unhandled param kind: Kind.${Kind[param.type.kind]}`,
                    );
            }
        }

        this.arg_stack_counter = 0;

        return args;
    }

    public dispose(): void {
        this.arg_stack.free();
    }
}
