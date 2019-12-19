import { VirtualMachine } from "./VirtualMachine";
import { AsmToken, Instruction, InstructionSegment, SegmentType } from "../instructions";
import { assert } from "../../../core/util";
import {
    OP_ARG_PUSHA,
    OP_ARG_PUSHB,
    OP_ARG_PUSHL,
    OP_ARG_PUSHR,
    OP_ARG_PUSHS,
    OP_ARG_PUSHW,
    OP_CALL,
    OP_SWITCH_CALL,
    OP_VA_CALL,
} from "../opcodes";
import { CallStackElement } from "./Thread";
import Logger from "js-logger";

const logger = Logger.get("quest_editor/scripting/vm/Debugger");

/**
 * Contains all logic pertaining to breakpoints and stepping through code.
 */
export class Debugger {
    private readonly vm: VirtualMachine;
    private break_on_next = false;
    /**
     * Invisible breakpoints that help with stepping over/in/out.
     */
    private readonly stepping_breakpoints: number[] = [];
    private readonly _breakpoints: number[] = [];

    readonly breakpoints: readonly number[] = this._breakpoints;

    constructor(vm: VirtualMachine) {
        this.vm = vm;
    }

    step_over(): void {
        const execloc = this.vm.get_current_call_stack_element();

        const src_segment = this.get_instruction_segment_by_index(execloc.seg_idx);
        const src_instr = src_segment.instructions[execloc.inst_idx];
        const dst_label = this.get_step_innable_instruction_label_argument(src_instr);

        // nothing to step over, just break on next instruction
        if (dst_label === undefined) {
            this.break_on_next = true;
        }
        // set a breakpoint on the next line
        else {
            const dst_srcloc = this.get_next_source_location(execloc);

            // set breakpoint
            if (dst_srcloc) {
                this.stepping_breakpoints.push(dst_srcloc.line_no);
            }
        }
    }

    step_in(): void {
        const execloc = this.vm.get_current_call_stack_element();
        const src_segment = this.get_instruction_segment_by_index(execloc.seg_idx);
        const src_instr = src_segment.instructions[execloc.inst_idx];
        const dst_label = this.get_step_innable_instruction_label_argument(src_instr);

        // not a step-innable instruction, behave like step-over
        if (dst_label === undefined) {
            this.step_over();
        }
        // can step-in
        else {
            const dst_segment = this.get_instruction_segment_by_label(dst_label);
            const dst_instr = dst_segment.instructions[0];
            const dst_srcloc = this.get_source_location(dst_instr);

            if (dst_srcloc) {
                this.stepping_breakpoints.push(dst_srcloc.line_no);
            }
        }
    }

    step_out(): void {
        throw new Error("Not implemented.");
    }

    set_breakpoint(line_no: number): boolean {
        if (!this._breakpoints.includes(line_no)) {
            this._breakpoints.push(line_no);
            return true;
        } else {
            return false;
        }
    }

    remove_breakpoint(line_no: number): boolean {
        const index = this._breakpoints.indexOf(line_no);

        if (index != -1) {
            this._breakpoints.splice(index, 1);
            return true;
        } else {
            return false;
        }
    }

    toggle_breakpoint(line_no: number): void {
        const index = this._breakpoints.indexOf(line_no);

        if (index == -1) {
            this._breakpoints.push(line_no);
        } else {
            this._breakpoints.splice(index, 1);
        }
    }

    clear_breakpoints(): void {
        this._breakpoints.splice(0, Infinity);
    }

    should_pause(srcloc: AsmToken): boolean {
        const breakpoint_hit = this._breakpoints.includes(srcloc.line_no);

        if (breakpoint_hit) {
            logger.debug(`Breakpoint hit at line ${srcloc.line_no}.`);
        }

        const pause =
            this.break_on_next ||
            breakpoint_hit ||
            this.stepping_breakpoints.includes(srcloc.line_no);

        this.break_on_next = false;

        if (pause) {
            this.stepping_breakpoints.length = 0;
        }

        return pause;
    }

    reset(): void {
        this.stepping_breakpoints.splice(0, Infinity);
    }

    private get_instruction_segment_by_label(label: number): InstructionSegment {
        const seg_idx = this.vm.get_segment_index_by_label(label);
        return this.get_instruction_segment_by_index(seg_idx);
    }

    private get_instruction_segment_by_index(index: number): InstructionSegment {
        const segment = this.vm.object_code[index];

        assert(
            segment.type === SegmentType.Instructions,
            `Expected segment ${index} to be of type ${
                SegmentType[SegmentType.Instructions]
            }, but was ${SegmentType[segment.type]}.`,
        );

        return segment;
    }

    private get_step_innable_instruction_label_argument(inst: Instruction): number | undefined {
        switch (inst.opcode.code) {
            case OP_VA_CALL.code:
            case OP_CALL.code:
                return inst.args[0].value;
            case OP_SWITCH_CALL.code:
                return inst.args[1].value;
        }
    }

    private get_next_source_location(execloc: CallStackElement): AsmToken | undefined {
        const next_loc = new CallStackElement(execloc.seg_idx, execloc.inst_idx);
        const segment = this.vm.object_code[next_loc.seg_idx];

        // can't go to non-code segments
        if (segment.type !== SegmentType.Instructions) {
            return undefined;
        }

        // move to next instruction
        // move to next segment if segment ended
        if (++next_loc.inst_idx >= segment.instructions.length) {
            next_loc.seg_idx++;
            next_loc.inst_idx = 0;
        }

        // no more segments
        if (next_loc.seg_idx >= this.vm.object_code.length) {
            return undefined;
        }

        const dst_instr = segment.instructions[next_loc.inst_idx];
        return this.get_source_location(dst_instr);
    }

    private get_source_location(inst: Instruction): AsmToken | undefined {
        let dst_srcloc = inst.asm?.mnemonic;

        // use the location of the arg of the arg_push opcode instead
        if (this.is_arg_push_opcode(inst)) {
            dst_srcloc = inst.asm?.args[0];
        }

        return dst_srcloc;
    }

    private is_arg_push_opcode(inst: Instruction): boolean {
        switch (inst.opcode.code) {
            case OP_ARG_PUSHB.code:
            case OP_ARG_PUSHL.code:
            case OP_ARG_PUSHR.code:
            case OP_ARG_PUSHW.code:
            case OP_ARG_PUSHA.code:
            case OP_ARG_PUSHS.code:
                return true;
        }
        return false;
    }
}
