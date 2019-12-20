import { PauseAtType, VirtualMachine } from "./VirtualMachine";
import { SegmentType } from "../instructions";
import { InstructionPointer } from "./InstructionPointer";

/**
 * Ensures consistency between source-level breakpoints and VM breakpoints.
 */
export class Debugger {
    private readonly vm: VirtualMachine;
    private readonly _breakpoints: Breakpoint[] = [];
    /**
     * Invisible breakpoint to help with stepping over/in/out.
     */
    private stepping_breakpoint?: Breakpoint;

    readonly breakpoints: readonly Breakpoint[] = this._breakpoints;

    constructor(vm: VirtualMachine) {
        this.vm = vm;
    }

    resume(): void {
        this.stepping_breakpoint?.deactivate();
        this.stepping_breakpoint = undefined;

        this.vm.resume({ type: PauseAtType.NextBreakPoint });
    }

    step_over(): void {
        const frame = this.vm.get_current_stack_frame();

        if (frame) {
            this.vm.resume({ type: PauseAtType.StackFrame, frame });
        }
    }

    step_in(): void {
        this.vm.resume({ type: PauseAtType.Instruction });
    }

    step_out(): void {
        throw new Error("Not implemented.");
    }

    set_breakpoint(line_no: number): boolean {
        if (this._breakpoints.findIndex(bp => bp.line_no === line_no) === -1) {
            this._breakpoints.push(new Breakpoint(line_no, undefined, this.vm));
            return true;
        } else {
            return false;
        }
    }

    remove_breakpoint(line_no: number): boolean {
        const index = this._breakpoints.findIndex(bp => bp.line_no === line_no);

        if (index != -1) {
            this._breakpoints.splice(index, 1)[0].deactivate();
            return true;
        } else {
            return false;
        }
    }

    toggle_breakpoint(line_no: number): void {
        const index = this._breakpoints.findIndex(bp => bp.line_no === line_no);

        if (index == -1) {
            this._breakpoints.push(new Breakpoint(line_no, undefined, this.vm));
        } else {
            this._breakpoints.splice(index, 1)[0].deactivate();
        }
    }

    clear_breakpoints(): void {
        for (const bp of this._breakpoints.splice(0, Infinity)) {
            bp.deactivate();
        }
    }

    reset(): void {
        this.stepping_breakpoint?.deactivate();
        this.stepping_breakpoint = undefined;

        for (const bp of this._breakpoints) {
            bp.activate();
        }
    }
}

export class Breakpoint {
    constructor(
        readonly line_no: number,
        private ptr: InstructionPointer | undefined,
        private vm: VirtualMachine,
    ) {
        if (ptr == undefined) {
            this.activate();
        } else {
            this.vm.set_breakpoint(ptr);
        }
    }

    get active(): boolean {
        return this.ptr != undefined;
    }

    activate(): void {
        this.ptr = this.line_no_to_inst_pointer(this.line_no);

        if (this.ptr) {
            this.vm.set_breakpoint(this.ptr);
        }
    }

    deactivate(): void {
        if (this.ptr) {
            this.vm.remove_breakpoint(this.ptr);
        }
    }

    private line_no_to_inst_pointer(line_no: number): InstructionPointer | undefined {
        if (this.vm.halted) return undefined;

        for (let seg_idx = 0; seg_idx < this.vm.object_code.length; seg_idx++) {
            const segment = this.vm.object_code[seg_idx];

            if (segment.type === SegmentType.Instructions) {
                for (let inst_idx = 0; inst_idx < segment.instructions.length; inst_idx++) {
                    const inst = segment.instructions[inst_idx];

                    if (inst.asm?.mnemonic?.line_no === line_no) {
                        return new InstructionPointer(seg_idx, inst_idx, this.vm.object_code);
                    }
                }
            }
        }

        return undefined;
    }
}
