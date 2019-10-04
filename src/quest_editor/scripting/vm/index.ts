import {
    Arg,
    Instruction,
    InstructionSegment,
    new_arg,
    Segment,
    SegmentType,
} from "../instructions";
import {
    OP_ADD,
    OP_ADDI,
    OP_AND,
    OP_ANDI,
    OP_ARG_PUSHB,
    OP_ARG_PUSHL,
    OP_ARG_PUSHR,
    OP_ARG_PUSHS,
    OP_ARG_PUSHW,
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

const numeric_ops: Record<
    "add" | "sub" | "mul" | "div" | "idiv" | "mod" | "and" | "or" | "xor" | "shl" | "shr",
    BinaryNumericOperation
> = {
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

type ComparisonOperation = (a: number, b: number) => boolean;

const comparison_ops: Record<"eq" | "neq" | "gt" | "lt" | "gte" | "lte", ComparisonOperation> = {
    eq: (a, b) => a === b,
    neq: (a, b) => a !== b,
    gt: (a, b) => a > b,
    lt: (a, b) => a < b,
    gte: (a, b) => a >= b,
    lte: (a, b) => a <= b,
};

/**
 * Short-circuiting fold.
 */
function andfold<T, A>(fn: (acc: A, cur: T) => A | null, init: A, lst: T[]): A | null {
    let acc = init;

    for (const item of lst) {
        const new_val = fn(acc, item);

        if (new_val === null) {
            return null;
        } else {
            acc = new_val;
        }
    }

    return acc;
}

/**
 * Short-circuiting reduce.
 */
function andreduce<T>(fn: (acc: T, cur: T) => T | null, lst: T[]): T | null {
    return andfold(fn, lst[0], lst.slice(1));
}

/**
 * Applies the given arguments to the given function.
 * Returns the second argument if the function returns a truthy value, else null.
 */
function andsecond<T>(fn: (first: T, second: T) => any, first: T, second: T): T | null {
    if (fn(first, second)) {
        return second;
    }
    return null;
}

function rest<T>(lst: T[]): T[] {
    return lst.slice(1);
}

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

        const arg_vals = inst.args.map(arg => arg.value);
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
                this.push_arg_stack(exec, new_arg(this.get_sint(arg0), REGISTER_SIZE));
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
            // conditional jumps
            case OP_JMP_ON.code:
                // all eq 1?
                this.conditional_jump(
                    exec,
                    arg0,
                    comparison_ops.eq,
                    1,
                    ...rest(arg_vals).map(reg => this.get_sint(reg)),
                );
                break;
            case OP_JMP_OFF.code:
                // all eq 0?
                this.conditional_jump(
                    exec,
                    arg0,
                    comparison_ops.eq,
                    0,
                    ...rest(arg_vals).map(reg => this.get_sint(reg)),
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

    private get_uint(reg: number): number {
        return this.registers.getUint32(REGISTER_SIZE * reg);
    }

    private set_uint(reg: number, value: number): void {
        this.registers.setUint32(REGISTER_SIZE * reg, value);
    }

    private do_numeric_op_with_register(
        reg1: number,
        reg2: number,
        op: BinaryNumericOperation,
    ): void {
        this.do_numeric_op_with_literal(reg1, this.get_sint(reg2), op);
    }

    private do_numeric_op_with_literal(
        reg: number,
        literal: number,
        op: BinaryNumericOperation,
    ): void {
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

    private jump_to_label(exec: Thread, label: number): void {
        const top = exec.call_stack_top();
        const seg_idx = this.label_to_seg_idx.get(label);

        if (seg_idx == undefined) {
            logger.warn(`Invalid jump label: ${label}.`);
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
        this.conditional_jump(exec, label, condition, this.get_sint(reg1), this.get_sint(reg2));
    }

    private signed_conditional_jump_with_literal(
        exec: Thread,
        label: number,
        condition: ComparisonOperation,
        reg: number,
        literal: number,
    ): void {
        this.conditional_jump(exec, label, condition, this.get_sint(reg), literal);
    }

    private unsigned_conditional_jump_with_register(
        exec: Thread,
        label: number,
        condition: ComparisonOperation,
        reg1: number,
        reg2: number,
    ): void {
        this.conditional_jump(exec, label, condition, this.get_uint(reg1), this.get_uint(reg2));
    }

    private unsigned_conditional_jump_with_literal(
        exec: Thread,
        label: number,
        condition: ComparisonOperation,
        reg: number,
        literal: number,
    ): void {
        this.conditional_jump(exec, label, condition, this.get_uint(reg), literal);
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
        if (andreduce(chain_cmp, vals) !== null) {
            this.jump_to_label(exec, label);
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
