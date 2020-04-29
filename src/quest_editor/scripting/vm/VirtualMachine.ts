import { Segment, SegmentType } from "../../../core/data_formats/asm/instructions";
import {
    Kind,
    OP_ADD,
    OP_ADD_MSG,
    OP_ADDI,
    OP_AND,
    OP_ANDI,
    OP_ARG_PUSHA,
    OP_ARG_PUSHB,
    OP_ARG_PUSHL,
    OP_ARG_PUSHR,
    OP_ARG_PUSHS,
    OP_ARG_PUSHW,
    OP_BB_MAP_DESIGNATE,
    OP_CALL,
    OP_CLEAR,
    OP_DIV,
    OP_DIVI,
    OP_EXIT,
    OP_FADD,
    OP_FADDI,
    OP_FDIV,
    OP_FDIVI,
    OP_FLET,
    OP_FLETI,
    OP_FMUL,
    OP_FMULI,
    OP_FSUB,
    OP_FSUBI,
    OP_GET_RANDOM,
    OP_GETTIME,
    OP_JMP,
    OP_JMP_E,
    OP_JMP_G,
    OP_JMP_GE,
    OP_JMP_L,
    OP_JMP_LE,
    OP_JMP_NE,
    OP_JMP_OFF,
    OP_JMP_ON,
    OP_JMPI_E,
    OP_JMPI_G,
    OP_JMPI_GE,
    OP_JMPI_L,
    OP_JMPI_LE,
    OP_JMPI_NE,
    OP_LET,
    OP_LETA,
    OP_LETB,
    OP_LETI,
    OP_LETW,
    OP_LIST,
    OP_MAP_DESIGNATE,
    OP_MAP_DESIGNATE_EX,
    OP_MOD,
    OP_MODI,
    OP_MUL,
    OP_MULI,
    OP_NOP,
    OP_OR,
    OP_ORI,
    OP_P_DEAD_V3,
    OP_RET,
    OP_REV,
    OP_SET,
    OP_SET_EPISODE,
    OP_SET_FLOOR_HANDLER,
    OP_SHIFT_LEFT,
    OP_SHIFT_RIGHT,
    OP_STACK_POP,
    OP_STACK_POPM,
    OP_STACK_PUSH,
    OP_STACK_PUSHM,
    OP_SUB,
    OP_SUBI,
    OP_SYNC,
    OP_THREAD,
    OP_THREAD_STG,
    OP_UJMP_G,
    OP_UJMP_GE,
    OP_UJMP_L,
    OP_UJMP_LE,
    OP_UJMPI_G,
    OP_UJMPI_GE,
    OP_UJMPI_L,
    OP_UJMPI_LE,
    OP_WINDOW_MSG,
    OP_WINEND,
    OP_XOR,
    OP_XORI,
} from "../../../core/data_formats/asm/opcodes";
import {
    andreduce,
    andsecond,
    BinaryNumericOperation,
    ComparisonOperation,
    numeric_ops,
    rest,
} from "./utils";
import { DefaultVirtualMachineIO, VirtualMachineIO } from "./io";
import { Episode } from "../../../core/data_formats/parsing/quest/Episode";
import { Endianness } from "../../../core/data_formats/Endianness";
import { Random } from "./Random";
import { Memory } from "./Memory";
import { InstructionPointer } from "./InstructionPointer";
import { StepMode, Thread } from "./Thread";
import { LogManager } from "../../../core/Logger";

export const REGISTER_COUNT = 256;

const REGISTER_SIZE = 4;
const VARIABLE_STACK_LENGTH = 16; // TODO: verify this value
const STRING_ARG_STORE_ADDRESS = 0x00a92700;
const STRING_ARG_STORE_SIZE = 1024; // TODO: verify this value
const ENTRY_SEGMENT = 0;
const LIST_ITEM_DELIMITER = "\n";

const logger = LogManager.get("quest_editor/scripting/vm/VirtualMachine");

export enum ExecutionResult {
    /**
     * There are no live threads, nothing to do.
     */
    Suspended,
    /**
     * Execution is paused due to hitting a breakpoint or because the VM is executing in
     * stepping mode.
     */
    Paused,
    /**
     * All threads have yielded.
     */
    WaitingVsync,
    /**
     * Waiting for any keypress. No method call required.
     */
    WaitingInput,
    /**
     * Waiting for a value to be selected in a list.
     * Call `list_select` to set selection.
     */
    WaitingSelection,
    /**
     * Execution has halted because the VM encountered an `exit` instruction, a fatal error was
     * raised or the VM was halted from outside.
     */
    Halted,
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

/**
 * This class emulates the PSO script engine. It's in charge of memory, threading and executing
 * instructions.
 */
export class VirtualMachine {
    // Quest details.

    private episode: Episode = Episode.I;
    private _object_code: readonly Segment[] = [];
    private readonly label_to_seg_idx: Map<number, number> = new Map();

    // VM state.

    private readonly registers = new Memory(REGISTER_COUNT * REGISTER_SIZE, Endianness.Little);
    private string_arg_store = "";
    private threads: Thread[] = [];
    private thread_idx = 0;
    private window_msg_open = false;
    private set_episode_called = false;
    private list_open = false;
    private selection_reg = 0;
    private _halted = true;
    // VM will ignore pauses until after this line number is hit.
    private ignore_pauses_until_after_line: number | undefined = undefined;

    // Debugging.

    private readonly breakpoints: InstructionPointer[] = [];
    private paused = false;
    private debugging_thread_id: number | undefined = undefined;

    /**
     * Set of unsupported opcodes that have already been logged. Each unsupported opcode will only
     * be logged once to avoid flooding the log with duplicate log messages.
     */
    private readonly unsupported_opcodes_logged: Set<number> = new Set();

    get object_code(): readonly Segment[] {
        return this._object_code;
    }

    get halted(): boolean {
        return this._halted;
    }

    set step_mode(step_mode: StepMode) {
        if (step_mode != undefined) {
            const thread = this.threads.find(thread => thread.id === this.debugging_thread_id);

            if (thread) {
                thread.step_mode = step_mode;
            }
        }
    }

    constructor(
        private io: VirtualMachineIO = new DefaultVirtualMachineIO(),
        private random: Random = new Random(),
    ) {}

    /**
     * Halts and resets the VM, then loads new object code.
     */
    load_object_code(object_code: readonly Segment[], episode: Episode): void {
        this.halt();

        logger.debug("Starting.");

        this._object_code = object_code;
        this.episode = episode;

        this.label_to_seg_idx.clear();
        let i = 0;

        for (const segment of this._object_code) {
            for (const label of segment.labels) {
                this.label_to_seg_idx.set(label, i);
            }

            i++;
        }

        this._halted = false;
    }

    /**
     * Schedules concurrent execution of the code at the given label.
     *
     * @param label - instruction label to start thread execution at.
     * @param area_id - if an area_id is passed in, the thread is floor-local.
     */
    start_thread(label: number, area_id?: number): void {
        const seg_idx = this.get_segment_index_by_label(label);
        const segment = this._object_code[seg_idx];

        if (segment.type !== SegmentType.Instructions) {
            throw new Error(
                `Label ${label} points to a ${SegmentType[segment.type]} segment, expecting ${
                    SegmentType[SegmentType.Instructions]
                }.`,
            );
        }

        const thread = new Thread(
            this.io,
            new InstructionPointer(seg_idx!, 0, this.object_code),
            area_id,
        );
        if (this.debugging_thread_id === undefined) {
            this.debugging_thread_id = thread.id;
        }
        this.threads.push(thread);
    }

    /**
     * Executes instructions while possible.
     */
    execute(): ExecutionResult {
        if (this._halted) {
            return ExecutionResult.Halted;
        }

        let inst_ptr: InstructionPointer | undefined;

        try {
            // Limit amount of instructions executed to prevent infinite loops.
            let execution_counter = 0;

            while (execution_counter++ < 10_000) {
                // Check whether VM is waiting for vsync or is suspended.
                if (this.threads.length >= 1 && this.thread_idx >= this.threads.length) {
                    return ExecutionResult.WaitingVsync;
                }

                const thread = this.current_thread();

                if (!thread) {
                    this.ignore_pauses_until_after_line = undefined;
                    return ExecutionResult.Suspended;
                }

                // This thread is the one currently selected for debugging?
                const debugging_current_thread = thread.id === this.debugging_thread_id;

                // Get current instruction.
                const frame = thread.current_stack_frame()!;
                inst_ptr = frame.instruction_pointer;
                const inst = inst_ptr.instruction;

                // Check whether the VM needs to pause only if it's not already paused. In that case
                // it's resuming.
                if (!this.paused) {
                    switch (thread.step_mode) {
                        // Always pause on breakpoints regardless of selected thread.
                        case StepMode.BreakPoint:
                            if (this.breakpoints.findIndex(bp => bp.equals(inst_ptr!)) !== -1) {
                                this.paused = true;
                                this.debugging_thread_id = thread.id;
                                // A breakpoint should interrupt the pause ignoring process
                                // since we are now probably in a different execution location.
                                this.ignore_pauses_until_after_line = undefined;
                                return ExecutionResult.Paused;
                            }
                            break;

                        // Only pause on steps if we are in the currently selected thread.
                        case StepMode.Over:
                            if (
                                debugging_current_thread &&
                                this.ignore_pauses_until_after_line === undefined &&
                                thread.step_frame &&
                                frame.idx <= thread.step_frame.idx &&
                                inst.asm?.mnemonic
                            ) {
                                this.paused = true;
                                return ExecutionResult.Paused;
                            }
                            break;

                        case StepMode.In:
                            if (
                                debugging_current_thread &&
                                this.ignore_pauses_until_after_line === undefined &&
                                inst.asm?.mnemonic
                            ) {
                                this.paused = true;
                                return ExecutionResult.Paused;
                            }
                            break;

                        case StepMode.Out:
                            if (
                                debugging_current_thread &&
                                this.ignore_pauses_until_after_line === undefined &&
                                thread.step_frame &&
                                frame.idx < thread.step_frame.idx &&
                                inst.asm?.mnemonic
                            ) {
                                this.paused = true;
                                return ExecutionResult.Paused;
                            }
                            break;
                    }

                    // Reached line, allow pausing again.
                    if (
                        debugging_current_thread &&
                        this.ignore_pauses_until_after_line ===
                            this.get_instruction_pointer(this.debugging_thread_id)?.source_location
                                ?.line_no
                    ) {
                        this.ignore_pauses_until_after_line = undefined;
                    }
                }

                // Not paused, the next instruction can be executed.
                this.paused = false;

                const result = this.execute_instruction(thread, inst_ptr);

                // Only return WaitingVsync when all threads have yielded.
                if (result != undefined && result !== ExecutionResult.WaitingVsync) {
                    return result;
                }
            }

            throw new Error(
                "Maximum execution count reached. The code probably contains an infinite loop.",
            );
        } catch (thrown) {
            let err = thrown;

            if (!(err instanceof Error)) {
                err = new Error(String(err));
            }

            try {
                this.io.error(err, inst_ptr);
            } finally {
                this.halt();
            }

            return ExecutionResult.Halted;
        }
    }

    /**
     * Signal to the VM that a vsync has happened.
     */
    vsync(): void {
        if (this.thread_idx >= this.threads.length) {
            this.thread_idx = 0;
        }
    }

    /**
     * Halts execution of all threads.
     */
    halt(): void {
        if (!this._halted) {
            logger.debug("Halting.");

            this.registers.zero();
            this.string_arg_store = "";
            this.threads = [];
            this.thread_idx = 0;
            this.window_msg_open = false;
            this.set_episode_called = false;
            this.list_open = false;
            this.selection_reg = 0;
            this._halted = true;
            this.paused = false;
            this.breakpoints.splice(0, Infinity);
            this.debugging_thread_id = undefined;
            this.unsupported_opcodes_logged.clear();
            this.ignore_pauses_until_after_line = undefined;
            Thread.reset_id_counter();
        }
    }

    /**
     * @param thread_id - If argument not given returns current thread's instruction pointer.
     */
    get_instruction_pointer(thread_id?: number): InstructionPointer | undefined {
        const thread =
            thread_id === undefined
                ? this.current_thread()
                : this.threads.find(thread => thread.id === thread_id);
        return thread?.current_stack_frame()?.instruction_pointer;
    }

    get_segment_index_by_label(label: number): number {
        if (!this.label_to_seg_idx.has(label)) {
            throw new Error(`Invalid argument: No such label ${label}.`);
        }

        return this.label_to_seg_idx.get(label)!;
    }

    /**
     * When the list opcode is used, call this method to select a value in the list.
     */
    list_select(idx: number): void {
        if (!this.list_open) {
            throw new Error("list_select may not be called if there is no list open");
        }
        this.set_register_unsigned(this.selection_reg, idx);
    }

    set_breakpoint(breakpoint: InstructionPointer): void {
        if (this.breakpoints.findIndex(bp => bp.equals(breakpoint)) === -1) {
            this.breakpoints.push(breakpoint);
        }
    }

    remove_breakpoint(breakpoint: InstructionPointer): void {
        const index = this.breakpoints.findIndex(bp => bp.equals(breakpoint));

        if (index !== -1) {
            this.breakpoints.splice(index, 1);
        }
    }

    set_debugging_thread(thread_id: number): void {
        if (this.threads.find(thread => thread.id === thread_id)) {
            this.debugging_thread_id = thread_id;

            const ip = this.get_instruction_pointer(thread_id);

            if (thread_id === this.current_thread()?.id) {
                this.ignore_pauses_until_after_line = undefined;
            }
            // If switching away from the thread that is currently being executed
            // it will look like we are paused but actually the VM has not yet
            // processed the next instruction and is not yet actually paused.
            // We shall ignore the next pause to prevent the user from having
            // to press the step button twice.
            else {
                // Exists in source?
                if (ip && ip.source_location) {
                    this.ignore_pauses_until_after_line = ip.source_location.line_no;
                }
                // No source location. Belongs to another instruction?
                else if (ip && ip.instruction.asm && ip.instruction.asm.args.length > 0) {
                    this.ignore_pauses_until_after_line = ip.instruction.asm.args[0].line_no;
                }
                // No source location can be inferred.
                else {
                    this.ignore_pauses_until_after_line = undefined;
                }
            }
        }
    }

    get_debugging_thread_id(): number | undefined {
        return this.debugging_thread_id;
    }

    get_thread_ids(): number[] {
        return this.threads.map(thread => thread.id);
    }

    get_current_thread_id(): number | undefined {
        return this.current_thread()?.id;
    }

    private current_thread(): Thread | undefined {
        return this.threads[this.thread_idx];
    }

    private terminate_thread(thread_idx: number): void {
        const thread = this.threads[thread_idx];

        this.threads.splice(thread_idx, 1);

        if (thread.id === this.debugging_thread_id) {
            if (this.threads.length === 0) {
                this.debugging_thread_id = undefined;
            } else {
                this.debugging_thread_id = this.threads[0].id;
            }
        }

        if (this.thread_idx >= thread_idx && this.thread_idx > 0) {
            this.thread_idx--;
        }

        logger.debug(`Thread #${thread.id} terminated.`);
    }

    /**
     * Advance to the next instruction.
     */
    private advance(thread: Thread): void {
        const frame = thread.current_stack_frame();
        if (!frame) return; // Thread already terminated.

        const next = frame.instruction_pointer.next();

        if (next) {
            thread.set_current_instruction_pointer(next);
        } else {
            // Reached EOF.
            // Game will crash if call stack is not empty.
            if (thread.call_stack.length > 0) {
                throw new Error("Reached EOF but call stack was not empty");
            }
            thread.pop_call_stack();
            this.terminate_thread(this.thread_idx);
        }
    }

    private execute_instruction(
        thread: Thread,
        inst_ptr: InstructionPointer,
    ): ExecutionResult | undefined {
        const inst = inst_ptr.instruction;

        let result: ExecutionResult | undefined = undefined;
        let advance = true;

        const arg_vals = inst.args.map(arg => arg.value);
        const [arg0, arg1, arg2] = arg_vals;

        // previous instruction must've been `list`.
        // list may not exist after the instruction
        if (this.list_open) {
            this.list_open = false;
        }

        const stack_args = thread.fetch_args(inst_ptr);

        switch (inst.opcode.code) {
            case OP_NOP.code:
                break;
            case OP_RET.code:
                this.pop_call_stack(this.thread_idx);
                break;
            case OP_SYNC.code:
                result = ExecutionResult.WaitingVsync;
                this.advance(thread);
                this.thread_idx++;
                advance = false;
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
                this.push_call_stack(thread, arg0);
                advance = false;
                break;
            case OP_JMP.code:
                this.jump_to_label(thread, arg0);
                advance = false;
                break;
            case OP_ARG_PUSHR.code:
                // deref given register ref
                thread.push_arg(this.get_register_signed(arg0), Kind.DWord);
                break;
            case OP_ARG_PUSHL.code:
                thread.push_arg(inst.args[0].value, Kind.DWord);
                break;
            case OP_ARG_PUSHB.code:
                thread.push_arg(inst.args[0].value, Kind.Byte);
                break;
            case OP_ARG_PUSHW.code:
                thread.push_arg(inst.args[0].value, Kind.Word);
                break;
            case OP_ARG_PUSHA.code:
                // push address of register
                thread.push_arg(this.get_register_address(inst.args[0].value), Kind.DWord);
                break;
            case OP_ARG_PUSHS.code:
                if (typeof arg0 === "string") {
                    // process tags
                    const string_arg = this.parse_template_string(arg0);
                    // store string and push its address
                    this.string_arg_store = string_arg.slice(0, STRING_ARG_STORE_SIZE / 2);
                    thread.push_arg(STRING_ARG_STORE_ADDRESS, Kind.String);
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
                // all nonzero?
                this.conditional_jump(
                    thread,
                    arg0,
                    (_, b) => b !== 0,
                    1,
                    ...rest(arg_vals).map(reg => this.get_register_signed(reg)),
                );
                advance = false;
                break;
            case OP_JMP_OFF.code:
                // all eq 0?
                this.conditional_jump(
                    thread,
                    arg0,
                    (a, b) => a === b,
                    0,
                    ...rest(arg_vals).map(reg => this.get_register_signed(reg)),
                );
                advance = false;
                break;
            case OP_JMP_E.code:
                this.signed_cond_jump_with_register(thread, arg2, (a, b) => a === b, arg0, arg1);
                advance = false;
                break;
            case OP_JMPI_E.code:
                this.signed_cond_jump_with_literal(thread, arg2, (a, b) => a === b, arg0, arg1);
                advance = false;
                break;
            case OP_JMP_NE.code:
                this.signed_cond_jump_with_register(thread, arg2, (a, b) => a !== b, arg0, arg1);
                advance = false;
                break;
            case OP_JMPI_NE.code:
                this.signed_cond_jump_with_literal(thread, arg2, (a, b) => a !== b, arg0, arg1);
                advance = false;
                break;
            case OP_UJMP_G.code:
                this.unsigned_cond_jump_with_register(thread, arg2, (a, b) => a > b, arg0, arg1);
                advance = false;
                break;
            case OP_UJMPI_G.code:
                this.unsigned_cond_jump_with_literal(thread, arg2, (a, b) => a > b, arg0, arg1);
                advance = false;
                break;
            case OP_JMP_G.code:
                this.signed_cond_jump_with_register(thread, arg2, (a, b) => a > b, arg0, arg1);
                advance = false;
                break;
            case OP_JMPI_G.code:
                this.signed_cond_jump_with_literal(thread, arg2, (a, b) => a > b, arg0, arg1);
                advance = false;
                break;
            case OP_UJMP_L.code:
                this.unsigned_cond_jump_with_register(thread, arg2, (a, b) => a < b, arg0, arg1);
                advance = false;
                break;
            case OP_UJMPI_L.code:
                this.unsigned_cond_jump_with_literal(thread, arg2, (a, b) => a < b, arg0, arg1);
                advance = false;
                break;
            case OP_JMP_L.code:
                this.signed_cond_jump_with_register(thread, arg2, (a, b) => a < b, arg0, arg1);
                advance = false;
                break;
            case OP_JMPI_L.code:
                this.signed_cond_jump_with_literal(thread, arg2, (a, b) => a < b, arg0, arg1);
                advance = false;
                break;
            case OP_UJMP_GE.code:
                this.unsigned_cond_jump_with_register(thread, arg2, (a, b) => a >= b, arg0, arg1);
                advance = false;
                break;
            case OP_UJMPI_GE.code:
                this.unsigned_cond_jump_with_literal(thread, arg2, (a, b) => a >= b, arg0, arg1);
                advance = false;
                break;
            case OP_JMP_GE.code:
                this.signed_cond_jump_with_register(thread, arg2, (a, b) => a >= b, arg0, arg1);
                advance = false;
                break;
            case OP_JMPI_GE.code:
                this.signed_cond_jump_with_literal(thread, arg2, (a, b) => a >= b, arg0, arg1);
                advance = false;
                break;
            case OP_UJMP_LE.code:
                this.unsigned_cond_jump_with_register(thread, arg2, (a, b) => a <= b, arg0, arg1);
                advance = false;
                break;
            case OP_UJMPI_LE.code:
                this.unsigned_cond_jump_with_literal(thread, arg2, (a, b) => a <= b, arg0, arg1);
                advance = false;
                break;
            case OP_JMP_LE.code:
                this.signed_cond_jump_with_register(thread, arg2, (a, b) => a <= b, arg0, arg1);
                advance = false;
                break;
            case OP_JMPI_LE.code:
                this.signed_cond_jump_with_literal(thread, arg2, (a, b) => a <= b, arg0, arg1);
                advance = false;
                break;
            // variable stack operations
            case OP_STACK_PUSH.code:
                this.push_variable_stack(thread, arg0, 1);
                break;
            case OP_STACK_POP.code:
                this.pop_variable_stack(thread, arg0, 1);
                break;
            case OP_STACK_PUSHM.code:
                this.push_variable_stack(thread, arg0, arg1);
                break;
            case OP_STACK_POPM.code:
                this.pop_variable_stack(thread, arg0, arg1);
                break;
            case OP_LIST.code:
                if (!this.window_msg_open) {
                    const list_items = this.deref_string(stack_args[1]).split(LIST_ITEM_DELIMITER);

                    result = ExecutionResult.WaitingSelection;
                    this.list_open = true;
                    this.selection_reg = stack_args[0];
                    this.io.list(list_items);
                }
                break;
            case OP_WINDOW_MSG.code:
                if (!this.window_msg_open) {
                    const str = this.deref_string(stack_args[0]);

                    result = ExecutionResult.WaitingInput;
                    this.window_msg_open = true;
                    this.io.window_msg(str);
                }
                break;
            case OP_ADD_MSG.code:
                if (this.window_msg_open) {
                    const str = this.deref_string(stack_args[0]);

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
            case OP_P_DEAD_V3.code:
                this.set_register_signed(stack_args[0], this.io.p_dead_v3(stack_args[1]) ? 1 : 0);
                break;
            case OP_SET_FLOOR_HANDLER.code:
                this.io.set_floor_handler(stack_args[0], stack_args[1]);
                break;
            case OP_THREAD_STG.code:
                this.start_thread(arg0);
                break;
            case OP_MAP_DESIGNATE.code:
                this.io.map_designate(
                    this.get_register_signed(arg0),
                    this.get_register_signed(arg0 + 2),
                );
                break;
            case OP_MAP_DESIGNATE_EX.code:
                this.io.map_designate(
                    this.get_register_signed(arg0),
                    this.get_register_signed(arg0 + 3),
                );
                break;
            case OP_GET_RANDOM.code:
                {
                    const low = this.get_register_signed(arg0);
                    const hi = this.get_register_signed(arg0 + 1);

                    const r = this.random.next();
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
                    this.io.warning(
                        "Calling set_episode more than once is not supported.",
                        inst_ptr,
                    );
                    break;
                }

                this.set_episode_called = true;

                if (!this._object_code[inst_ptr.seg_idx].labels.includes(ENTRY_SEGMENT)) {
                    this.io.warning(
                        `Calling set_episode outside of segment ${ENTRY_SEGMENT} is not supported.`,
                        inst_ptr,
                    );
                    break;
                }

                if (encode_episode_number(this.episode) !== arg0) {
                    this.io.warning(
                        "Calling set_episode with an argument that does not " +
                            "match the quest's designated episode is not supported.",
                        inst_ptr,
                    );
                    break;
                }

                break;
            case OP_BB_MAP_DESIGNATE.code:
                this.io.map_designate(arg0, arg2);
                break;
            default:
                if (!this.unsupported_opcodes_logged.has(inst.opcode.code)) {
                    this.unsupported_opcodes_logged.add(inst.opcode.code);
                    this.io.warning(`Unsupported instruction.`, inst_ptr);
                }
                break;
        }

        if (advance) {
            this.advance(thread);
        }

        return result;
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

    private push_call_stack(thread: Thread, label: number): void {
        const seg_idx = this.get_segment_index_by_label(label);
        const segment = this._object_code[seg_idx];

        if (segment.type !== SegmentType.Instructions) {
            throw new Error(
                `Label ${label} points to a ${SegmentType[segment.type]} segment, expecting ${
                    SegmentType[SegmentType.Instructions]
                }.`,
            );
        } else {
            thread.push_frame(new InstructionPointer(seg_idx, 0, this.object_code));
        }
    }

    private pop_call_stack(idx: number): void {
        this.threads[idx].pop_call_stack();

        if (this.threads[idx].call_stack.length === 0) {
            // popped off the last return address
            // which means this is the end of the function this thread was started on
            // which means this is the end of this thread
            this.terminate_thread(idx);
        }
    }

    private jump_to_label(thread: Thread, label: number): void {
        thread.set_current_instruction_pointer(
            new InstructionPointer(this.get_segment_index_by_label(label), 0, this.object_code),
        );
    }

    private signed_cond_jump_with_register(
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

    private signed_cond_jump_with_literal(
        exec: Thread,
        label: number,
        condition: ComparisonOperation,
        reg: number,
        literal: number,
    ): void {
        this.conditional_jump(exec, label, condition, this.get_register_signed(reg), literal);
    }

    private unsigned_cond_jump_with_register(
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

    private unsigned_cond_jump_with_literal(
        exec: Thread,
        label: number,
        condition: ComparisonOperation,
        reg: number,
        literal: number,
    ): void {
        this.conditional_jump(exec, label, condition, this.get_register_unsigned(reg), literal);
    }

    private conditional_jump(
        thread: Thread,
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
            this.jump_to_label(thread, label);
        } else {
            this.advance(thread);
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

    private get_register_address(reg: number): number {
        return reg * REGISTER_SIZE;
    }

    private deref_string(address: number): string {
        if (address === STRING_ARG_STORE_ADDRESS) {
            return this.string_arg_store;
        }

        if (address > 0 && address < REGISTER_COUNT * REGISTER_SIZE) {
            return this.registers.string_utf16_at(address, REGISTER_COUNT * REGISTER_SIZE, true);
        }

        throw new Error(`Failed to dereference string: Invalid address ${address}`);
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
}
