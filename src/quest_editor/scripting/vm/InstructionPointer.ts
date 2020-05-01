import {
    AsmToken,
    Instruction,
    InstructionSegment,
    Segment,
    SegmentType,
} from "../../../core/data_formats/asm/instructions";
import { assert } from "../../../core/util";

export class InstructionPointer {
    readonly seg_idx: number;
    readonly inst_idx: number;
    private readonly object_code: readonly Segment[];

    constructor(seg_idx: number, inst_idx: number, object_code: readonly Segment[]) {
        assert(seg_idx >= 0, `seg_idx should be greater or equal to 0.`);
        assert(seg_idx < object_code.length, `seg_idx should be less than ${object_code.length}.`);

        const segment = object_code[seg_idx];

        assert(
            segment.type === SegmentType.Instructions,
            "seg_idx should point to an instructions segment.",
        );

        assert(inst_idx >= 0, `inst_idx should be greater or equal to 0.`);
        assert(
            inst_idx < segment.instructions.length,
            `inst_idx should be less than ${segment.instructions.length}.`,
        );

        this.seg_idx = seg_idx;
        this.inst_idx = inst_idx;
        this.object_code = object_code;
    }

    get segment(): InstructionSegment {
        return this.object_code[this.seg_idx] as InstructionSegment;
    }

    get instruction(): Instruction {
        return this.segment.instructions[this.inst_idx];
    }

    get source_location(): AsmToken | undefined {
        return this.instruction.asm?.mnemonic || this.instruction.asm?.args[0];
    }

    /**
     * @returns a pointer to the next instruction.
     */
    next(): InstructionPointer | undefined {
        if (this.inst_idx + 1 < this.segment.instructions.length) {
            return new InstructionPointer(this.seg_idx, this.inst_idx + 1, this.object_code);
        }

        // Segment ended, move to the next segment.
        if (this.seg_idx + 1 < this.object_code.length) {
            return new InstructionPointer(this.seg_idx + 1, 0, this.object_code);
        }

        // Reached EOF.
        return undefined;
    }

    equals(other: InstructionPointer): boolean {
        return this.seg_idx === other.seg_idx && this.inst_idx == other.inst_idx;
    }
}
