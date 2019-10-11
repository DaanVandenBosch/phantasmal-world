import { Instruction, InstructionSegment, Segment, SegmentType } from "../instructions";
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
    Param,
    Kind,
} from "../opcodes";
import Logger from "js-logger";
import { ArrayBufferCursor } from "../../../core/data_formats/cursor/ArrayBufferCursor";
import { Endianness } from "../../../core/data_formats/Endianness";

const logger = Logger.get("quest_editor/scripting/vm");

const REGISTERS_BASE_ADDRESS = 0x00a954b0;
const REGISTER_COUNT = 256;
const REGISTER_SIZE = 4;
const VARIABLE_STACK_LENGTH = 16; // TODO: verify this value
const ARG_STACK_SLOT_SIZE = 4;
const ARG_STACK_LENGTH = 8;
const STRING_ARG_STORE_ADDRESS = 0x00a92700;
const STRING_ARG_STORE_SIZE = 1024; // TODO: verify this value

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
function andfold<T, A>(fn: (acc: A, cur: T) => A | undefined, init: A, lst: T[]): A | undefined {
    let acc = init;

    for (const item of lst) {
        const new_val = fn(acc, item);

        if (new_val === undefined) {
            return undefined;
        } else {
            acc = new_val;
        }
    }

    return acc;
}

/**
 * Short-circuiting reduce.
 */
function andreduce<T>(fn: (acc: T, cur: T) => T | undefined, lst: T[]): T | undefined {
    return andfold(fn, lst[0], lst.slice(1));
}

/**
 * Applies the given arguments to the given function.
 * Returns the second argument if the function returns a truthy value, else undefined.
 */
function andsecond<T>(fn: (first: T, second: T) => any, first: T, second: T): T | undefined {
    if (fn(first, second)) {
        return second;
    }
    return undefined;
}

function rest<T>(lst: T[]): T[] {
    return lst.slice(1);
}

type Range = [number, number];

function ranges_overlap(a: Range, b: Range): boolean {
    return a[0] <= b[1] && b[0] <= a[1];
}

class VirtualMachineMemoryBuffer extends ArrayBufferCursor {
    /**
     * The memory this buffer belongs to.
     */
    public readonly memory: VirtualMachineMemory;
    /**
     * The memory address of this buffer.
     */
    public readonly address: number;

    constructor(memory: VirtualMachineMemory, address: number, size: number) {
        super(new ArrayBuffer(size), Endianness.Little);
        this.memory = memory;
        this.address = address;
    }

    public get_offset(byte_offset: number): VirtualMachineMemorySlot | undefined {
        return this.memory.get(this.address + byte_offset);
    }

    public free(): void {
        this.memory.free(this.address);
    }

    public zero(): void {
        new Uint32Array(this.backing_buffer).fill(0);
    }
}

/**
 * Represents a single location in memory.
 */
class VirtualMachineMemorySlot {
    /**
     * The memory this slot belongs to.
     */
    public readonly memory: VirtualMachineMemory;
    /**
     * The memory address this slots represents.
     */
    public readonly address: number;
    /**
     * The allocated buffer this slot is a part of.
     */
    public readonly buffer: VirtualMachineMemoryBuffer;
    /**
     * The offset that this slot represents in the buffer.
     */
    public readonly byte_offset: number;

    constructor(
        memory: VirtualMachineMemory,
        address: number,
        buffer: VirtualMachineMemoryBuffer,
        byte_offset: number,
    ) {
        this.memory = memory;
        this.address = address;
        this.buffer = buffer;
        this.byte_offset = byte_offset;
    }
}

/**
 * Maps memory addresses to buffers.
 */
class VirtualMachineMemory {
    private allocated_ranges: Range[] = [];
    private ranges_sorted: boolean = true;
    private memory: Map<number, VirtualMachineMemorySlot> = new Map();

    private sort_ranges(): void {
        this.allocated_ranges.sort((a, b) => a[0] - b[0]);

        this.ranges_sorted = true;
    }

    /**
     * Would a buffer of the given size fit at the given address?
     */
    private will_fit(address: number, size: number): boolean {
        const fit_range: Range = [address, address + size - 1];

        if (!this.ranges_sorted) {
            this.sort_ranges();
        }

        // check if it would overlap any already allocated space
        for (const alloc_range of this.allocated_ranges) {
            if (ranges_overlap(alloc_range, fit_range)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Returns an address where a buffer of the given size would fit.
     */
    private find_free_space(size: number): number {
        let address = 0;

        // nothing yet allocated, we can place it wherever
        if (this.allocated_ranges.length < 1) {
            return address;
        }

        if (!this.ranges_sorted) {
            this.sort_ranges();
        }

        // check if buffer could fit in between allocated buffers
        for (const alloc_range of this.allocated_ranges) {
            if (!ranges_overlap(alloc_range, [address, address + size - 1])) {
                return address;
            }

            address = alloc_range[1] + 1;
        }

        // just place it at the end
        return address;
    }

    /**
     * Allocate a buffer of the given size at the given address.
     * If the address is omitted a suitable location is chosen.
     * @returns The allocated buffer.
     */
    public allocate(size: number, address?: number): VirtualMachineMemoryBuffer {
        if (size <= 0) {
            throw new Error("Allocation failed: The size of the buffer must be greater than 0");
        }

        // check if given address is good or find an address if none was given
        if (address === undefined) {
            address = this.find_free_space(size);
        } else {
            if (!this.will_fit(address, size)) {
                throw new Error(
                    "Allocation failed: Cannot fit a buffer of the given size at the given address",
                );
            }
        }

        // save the range of allocated memory
        this.allocated_ranges.push([address, address + size - 1]);
        this.ranges_sorted = false;

        // the actual buffer
        const buf = new VirtualMachineMemoryBuffer(this, address, size);

        // set addresses to correct buffer offsets
        for (let offset = 0; offset < size; offset++) {
            this.memory.set(
                address + offset,
                new VirtualMachineMemorySlot(this, address, buf, offset),
            );
        }

        return buf;
    }

    /**
     * Free the memory allocated for the buffer at the given address.
     */
    public free(address: number): void {
        // check if address is a valid allocated buffer
        let range: Range | undefined = undefined;
        let range_idx = -1;

        for (let i = 0; i < this.allocated_ranges.length; i++) {
            const cur = this.allocated_ranges[i];
            if (cur[0] === address) {
                range = cur;
                range_idx = i;
                break;
            }
        }

        if (range === undefined) {
            throw new Error("Free failed: Given address is not the start of an allocated buffer");
        }

        const [alloc_start, alloc_end] = range;

        // remove addresses
        for (let addr = alloc_start; addr <= alloc_end; addr++) {
            this.memory.delete(addr);
        }

        // remove range
        this.allocated_ranges.splice(range_idx, 1);
    }

    /**
     * Gets the memory at the given address. Returns undefined if
     * there is nothing allocated at the given address.
     */
    public get(address: number): VirtualMachineMemorySlot | undefined {
        if (this.memory.has(address)) {
            return this.memory.get(address)!;
        }

        return undefined;
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

        this.thread.push(
            new Thread(
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
     *
     * @returns true if an instruction was executed, false otherwise.
     */
    execute(): ExecutionResult {
        if (this.thread.length === 0) return ExecutionResult.Halted;
        if (this.thread_idx >= this.thread.length) return ExecutionResult.WaitingVsync;

        const exec = this.thread[this.thread_idx];
        const inst = this.get_next_instruction_from_thread(exec);

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
                exec.push_arg(this.get_sint(arg0), Kind.DWord);
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
                {
                    // store string and push its address
                    const string_arg = arg0 as string;
                    this.string_arg_store.write_string_utf16_at(
                        0,
                        string_arg,
                        string_arg.length * 2,
                    );
                    exec.push_arg(this.string_arg_store.address, Kind.String);
                }
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
                    this.dispose_thread(this.thread_idx);
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
        return this.registers.i32_at(REGISTER_SIZE * reg);
    }

    private set_sint(reg: number, value: number): void {
        this.registers.write_i32_at(REGISTER_SIZE * reg, value);
    }

    private get_uint(reg: number): number {
        return this.registers.u32_at(REGISTER_SIZE * reg);
    }

    private set_uint(reg: number, value: number): void {
        this.registers.write_u32_at(REGISTER_SIZE * reg, value);
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
            exec.variable_stack.push(this.get_uint(r));
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
            this.set_uint(r, exec.variable_stack.pop()!);
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

    private clear_registers(): void {
        this.registers.zero();
    }

    private get_register_address(reg: number): number {
        return this.registers.address + reg * REGISTER_SIZE;
    }
}

class ExecutionLocation {
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

    constructor(next: ExecutionLocation, arg_stack: VirtualMachineMemoryBuffer, global: boolean) {
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

    public fetch_args(params: readonly Param[]): number[] {
        const args: number[] = [];

        if (params.length !== this.arg_stack_counter) {
            logger.warn("Argument stack: Argument count mismatch");
        }

        for (let i = 0; i < params.length; i++) {
            const param = params[i];

            if (param.type.kind !== this.arg_stack_types[i]) {
                logger.warn("Argument stack: Argument type mismatch");
            }

            switch (param.type.kind) {
                case Kind.Byte:
                    args.push(this.arg_stack.u8_at(i * ARG_STACK_SLOT_SIZE));
                    break;
                case Kind.Word:
                    args.push(this.arg_stack.u16_at(i * ARG_STACK_SLOT_SIZE));
                    break;
                case Kind.DWord:
                case Kind.String:
                    args.push(this.arg_stack.u32_at(i * ARG_STACK_SLOT_SIZE));
                    break;
                default:
                    throw new Error(`Unhandled param kind: Kind.${Kind[param.type.kind]}`);
            }
        }

        this.arg_stack_counter = 0;

        return args;
    }

    public dispose(): void {
        this.arg_stack.free();
    }
}
