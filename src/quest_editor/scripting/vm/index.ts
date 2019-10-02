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

        switch (inst.opcode) {
            case OP_NOP:
                break;
            case OP_RET:
                this.pop_call_stack(this.thread_idx, exec);
                break;
            case OP_SYNC:
                this.thread_idx++;
                break;
            case OP_EXIT:
                this.halt();
                break;
            case OP_THREAD:
                this.start_thread(inst.args[0].value);
                break;
            case OP_LET:
                this.set_sint(inst.args[0].value, this.get_sint(inst.args[1].value));
                break;
            case OP_LETI:
                this.set_sint(inst.args[0].value, inst.args[1].value);
                break;
            case OP_LETB:
            case OP_LETW:
                this.set_uint(inst.args[0].value, inst.args[1].value);
                break;
            case OP_SET:
                this.set_sint(inst.args[0].value, 1);
                break;
            case OP_CLEAR:
                this.set_sint(inst.args[0].value, 0);
                break;
            case OP_REV:
                this.set_sint(inst.args[0].value, this.get_sint(inst.args[0].value) === 0 ? 1 : 0);
                break;
            case OP_CALL:
                this.push_call_stack(exec, inst.args[0].value);
                break;
            case OP_JMP:
                this.jump_to_label(exec, inst.args[0].value);
                break;
            case OP_ARG_PUSHR:
                // deref given register ref
                this.push_arg_stack(exec, new_arg(
                    this.get_sint(inst.args[0].value),
                    REGISTER_SIZE,
                    inst.args[0].asm
                ));
                break;
            case OP_ARG_PUSHL:
            case OP_ARG_PUSHB:
            case OP_ARG_PUSHW:
            case OP_ARG_PUSHS:
                // push arg as-is
                this.push_arg_stack(exec, inst.args[0].value);
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
