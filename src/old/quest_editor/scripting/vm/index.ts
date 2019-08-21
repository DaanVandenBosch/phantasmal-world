import { Instruction, InstructionSegment, Segment, SegmentType } from "../instructions";
import { Opcode } from "../opcodes";
import Logger from "js-logger";

const logger = Logger.get("scripting/vm");

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

        this.thread.push(new Thread(new StackElement(seg_idx!, 0), true));
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
            case Opcode.NOP:
                break;
            case Opcode.RET:
                this.pop_call_stack(this.thread_idx, exec);
                break;
            case Opcode.SYNC:
                this.thread_idx++;
                break;
            case Opcode.EXIT:
                this.halt();
                break;
            case Opcode.THREAD:
                this.start_thread(inst.args[0].value);
                break;
            case Opcode.LET:
                this.set_sint(inst.args[0].value, this.get_sint(inst.args[1].value));
                break;
            case Opcode.LETI:
                this.set_sint(inst.args[0].value, inst.args[1].value);
                break;
            case Opcode.LETB:
            case Opcode.LETW:
                this.set_uint(inst.args[0].value, inst.args[1].value);
                break;
            case Opcode.SET:
                this.set_sint(inst.args[0].value, 1);
                break;
            case Opcode.CLEAR:
                this.set_sint(inst.args[0].value, 0);
                break;
            case Opcode.REV:
                this.set_sint(inst.args[0].value, this.get_sint(inst.args[0].value) === 0 ? 1 : 0);
                break;
            case Opcode.CALL:
                this.push_call_stack(exec, inst.args[0].value);
                break;
            default:
                throw new Error(`Unsupported instruction: ${inst.opcode.mnemonic}.`);
        }

        if (exec.stack.length) {
            const top = exec.stack_top();
            const segment = this.object_code[top.seg_idx] as InstructionSegment;

            if (++top.inst_idx >= segment.instructions.length) {
                top.seg_idx++;
                top.inst_idx = 0;
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
                exec.stack.push(new StackElement(seg_idx, -1));
            }
        }
    }

    private pop_call_stack(idx: number, exec: Thread): void {
        exec.stack.pop();

        if (exec.stack.length >= 1) {
            const top = exec.stack_top();
            const segment = this.object_code[top.seg_idx];

            if (!segment || segment.type !== SegmentType.Instructions) {
                throw new Error(`Invalid segment index ${top.seg_idx}.`);
            }
        } else {
            this.thread.splice(idx, 1);
        }
    }

    private get_next_instruction_from_thread(exec: Thread): Instruction {
        if (exec.stack.length) {
            const top = exec.stack_top();
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

class StackElement {
    constructor(public seg_idx: number, public inst_idx: number) {}
}

class Thread {
    /**
     * Call stack. The top element describes the instruction about to be executed.
     */
    public stack: StackElement[] = [];
    /**
     * Global or floor-local?
     */
    public global: boolean;

    stack_top(): StackElement {
        return this.stack[this.stack.length - 1];
    }

    constructor(next: StackElement, global: boolean) {
        this.stack = [next];
        this.global = global;
    }
}
