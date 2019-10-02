import { Instruction, InstructionSegment, Segment, SegmentType, Arg, new_arg } from "../instructions";
import {
    OP_CALL,
    OP_CLEAR,
    OP_EXIT,
    OP_LET,
    OP_LETB,
    OP_LETI,
    OP_LETW,
    OP_NOP,
    OP_RET,
    OP_REV,
    OP_SET,
    OP_SYNC,
    OP_THREAD,
    OP_JMP,
    OP_ARG_PUSHR,
    OP_ARG_PUSHL,
    OP_ARG_PUSHB,
    OP_ARG_PUSHW,
    OP_ARG_PUSHA,
    OP_ARG_PUSHO,
    OP_ARG_PUSHS,
    OP_ADD,
    OP_ADDI,
    OP_SUB,
    OP_SUBI,
    OP_FADD,
    OP_FADDI,
    OP_FSUB,
    OP_FSUBI,
    OP_FMUL,
    OP_MUL,
    OP_MULI,
    OP_FMULI,
    OP_DIV,
    OP_FDIV,
    OP_DIVI,
    OP_FDIVI,
    OP_MOD,
    OP_MODI,
    OP_AND,
    OP_ANDI,
    OP_OR,
    OP_ORI,
    OP_XOR,
    OP_XORI,
    OP_SHIFT_LEFT,
    OP_SHIFT_RIGHT,
} from "../opcodes";
import Logger from "js-logger";

const logger = Logger.get("quest_editor/scripting/vm");

const REGISTER_COUNT = 256;
const REGISTER_SIZE = 4;

export enum ExecutionResult {
    Ok,
    WaitingVsync,
    Halted,
}

type BinaryNumericOperation = (a: number, b: number) => number;

const numeric_ops: Record<"add" |
                          "sub" |
                          "mul" |
                          "div" |
                          "idiv" |
                          "mod" |
                          "and" |
                          "or" |
                          "xor" |
                          "shl" |
                          "shr",
                          BinaryNumericOperation> = {
    add: (a, b) => a + b,
    sub: (a, b) => a - b,
    mul: (a, b) => a * b,
    div: (a, b) => a / b,
    idiv: (a, b) => Math.floor(a / b),
    mod: (a, b) => a % b,
    and: (a, b) => a & b,
    or: (a, b) => a | b,
    xor: (a, b) => a ^ b,
    shl: (a, b) => a << b,
    shr: (a, b) => a >>> b,
};

export class VirtualMachine {
    private register_store = new ArrayBuffer(REGISTER_SIZE * REGISTER_COUNT);
    private register_uint8_view = new Uint8Array(this.register_store);
    private registers = new DataView(this.register_store);
    private object_code: Segment[] = [];
    private label_to_seg_idx: Map<number, number> = new Map();
    private thread: Thread[] = [];
    private thread_idx = 0;

    /**
     * Halts and resets the VM, then loads new object code.
     */
    load_object_code(object_code: Segment[]): void {
        this.halt();
        this.clear_registers();
        this.object_code = object_code;
        this.label_to_seg_idx.clear();
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

        this.thread.push(new Thread(new ExecutionLocation(seg_idx!, 0), true));
    }

    /**
     * Executes the next instruction if one is scheduled.
     *
     * @returns true if an instruction was executed, false otherwise.
     */
    execute(): ExecutionResult {
        if (this.thread.length === 0) return ExecutionResult.Halted;
        if (this.thread_idx >= this.thread.length) return ExecutionResult.WaitingVsync;

        const exec = this.thread[this.thread_idx];
        const inst = this.get_next_instruction_from_thread(exec);

        const [arg0, arg1, arg2, arg3, arg4, arg5, arg6, arg7] = inst.args.map(arg => arg.value);

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
            case OP_LET.code:
                this.set_sint(arg0, this.get_sint(arg1));
                break;
            case OP_LETI.code:
                this.set_sint(arg0, arg1);
                break;
            case OP_LETB.code:
            case OP_LETW.code:
                this.set_uint(arg0, arg1);
                break;
            case OP_SET.code:
                this.set_sint(arg0, 1);
                break;
            case OP_CLEAR.code:
                this.set_sint(arg0, 0);
                break;
            case OP_REV.code:
                this.set_sint(arg0, this.get_sint(arg0) === 0 ? 1 : 0);
                break;
            case OP_CALL.code:
                this.push_call_stack(exec, arg0);
                break;
            case OP_JMP.code:
                this.jump_to_label(exec, arg0);
                break;
            case OP_ARG_PUSHR.code:
                // deref given register ref
                this.push_arg_stack(exec, new_arg(
                    this.get_sint(arg0),
                    REGISTER_SIZE,
                    inst.args[0].asm
                ));
                break;
            case OP_ARG_PUSHL.code:
            case OP_ARG_PUSHB.code:
            case OP_ARG_PUSHW.code:
            case OP_ARG_PUSHS.code:
                // push arg as-is
                this.push_arg_stack(exec, inst.args[0]);
                break;
            // arithmetic operations
            case OP_ADD.code:
            case OP_FADD.code:
                this.do_numeric_op_with_register(arg0, arg1, numeric_ops.add);
                break;
            case OP_ADDI.code:
            case OP_FADDI.code:
                this.do_numeric_op_with_literal(arg0, arg1, numeric_ops.add);
                break;
            case OP_SUB.code:
            case OP_FSUB.code:
                this.do_numeric_op_with_register(arg0, arg1, numeric_ops.sub);
                break;
            case OP_SUBI.code:
            case OP_FSUBI.code:
                this.do_numeric_op_with_literal(arg0, arg1, numeric_ops.sub);
                break;
            case OP_MUL.code:
            case OP_FMUL.code:
                this.do_numeric_op_with_register(arg0, arg1, numeric_ops.mul);
                break;
            case OP_MULI.code:
            case OP_FMULI.code:
                this.do_numeric_op_with_literal(arg0, arg1, numeric_ops.mul);
                break;
            case OP_DIV.code:
                this.do_numeric_op_with_register(arg0, arg1, numeric_ops.idiv);
                break;
            case OP_FDIV.code:
                this.do_numeric_op_with_register(arg0, arg1, numeric_ops.div);
                break;
            case OP_DIVI.code:
                this.do_numeric_op_with_literal(arg0, arg1, numeric_ops.idiv);
                break;
            case OP_FDIVI.code:
                this.do_numeric_op_with_literal(arg0, arg1, numeric_ops.div);
                break;
            case OP_MOD.code:
                this.do_numeric_op_with_register(arg0, arg1, numeric_ops.mod);
                break;
            case OP_MODI.code:
                this.do_numeric_op_with_literal(arg0, arg1, numeric_ops.mod);
                break;
            // bit operations
            case OP_AND.code:
                this.do_numeric_op_with_register(arg0, arg1, numeric_ops.and);
                break;
            case OP_ANDI.code:
                this.do_numeric_op_with_literal(arg0, arg1, numeric_ops.and);
                break;
            case OP_OR.code:
                this.do_numeric_op_with_register(arg0, arg1, numeric_ops.or);
                break;
            case OP_ORI.code:
                this.do_numeric_op_with_literal(arg0, arg1, numeric_ops.or);
                break;
            case OP_XOR.code:
                this.do_numeric_op_with_register(arg0, arg1, numeric_ops.xor);
                break;
            case OP_XORI.code:
                this.do_numeric_op_with_literal(arg0, arg1, numeric_ops.xor);
                break;
            // shift operations
            case OP_SHIFT_LEFT.code:
                this.do_numeric_op_with_register(arg0, arg1, numeric_ops.shl);
                break;
            case OP_SHIFT_RIGHT.code:
                this.do_numeric_op_with_register(arg0, arg1, numeric_ops.shr);
                break;
            default:
                throw new Error(`Unsupported instruction: ${inst.opcode.mnemonic}.`);
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
                    this.thread.splice(this.thread_idx, 1);
                } else {
                    top.inst_idx = 0;
                }
            }
        }

        if (this.thread.length === 0) return ExecutionResult.Halted;
        if (this.thread_idx >= this.thread.length) return ExecutionResult.WaitingVsync;
        return ExecutionResult.Ok;
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
        this.thread = [];
        this.thread_idx = 0;
    }

    private get_sint(reg: number): number {
        return this.registers.getInt32(REGISTER_SIZE * reg);
    }

    private set_sint(reg: number, value: number): void {
        this.registers.setInt32(REGISTER_SIZE * reg, value);
    }

    private set_uint(reg: number, value: number): void {
        this.registers.setUint32(REGISTER_SIZE * reg, value);
    }

    private do_numeric_op_with_register(reg1: number, reg2: number, op: BinaryNumericOperation): void {
        this.do_numeric_op_with_literal(reg1, this.get_sint(reg2), op);
    }

    private do_numeric_op_with_literal(reg: number, literal: number, op: BinaryNumericOperation): void {
        this.set_sint(reg, op(this.get_sint(reg), literal));
    }

    private push_call_stack(exec: Thread, label: number): void {
        const seg_idx = this.label_to_seg_idx.get(label);

        if (seg_idx == undefined) {
            logger.warn(`Invalid label called: ${label}.`);
        } else {
            const segment = this.object_code[seg_idx];

            if (segment.type !== SegmentType.Instructions) {
                logger.warn(
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

    private jump_to_label(exec: Thread, label: number) {
        const top = exec.call_stack_top();
        const seg_idx = this.label_to_seg_idx.get(label);

        if (seg_idx == undefined) {
            logger.warn(`Invalid jump label: ${label}.`);
        } else {
            top.seg_idx = seg_idx;
            top.inst_idx = -1;
        }
    }

    private push_arg_stack(exec: Thread, arg: Arg): void {
        exec.arg_stack.push(arg);
    }

    private pop_arg_stack(exec: Thread): Arg {
        const arg = exec.arg_stack.pop();

        if (!arg) {
            throw new Error("Argument stack underflow.");
        }

        return arg;
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

    private clear_registers(): void {
        this.register_uint8_view.fill(0);
    }
}

class ExecutionLocation {
    constructor(public seg_idx: number, public inst_idx: number) {}
}

class Thread {
    /**
     * Call stack. The top element describes the instruction about to be executed.
     */
    public call_stack: ExecutionLocation[] = [];
    public arg_stack: Arg[] = [];
    /**
     * Global or floor-local?
     */
    public global: boolean;

    call_stack_top(): ExecutionLocation {
        return this.call_stack[this.call_stack.length - 1];
    }

    constructor(next: ExecutionLocation, global: boolean) {
        this.call_stack = [next];
        this.global = global;
    }
}
