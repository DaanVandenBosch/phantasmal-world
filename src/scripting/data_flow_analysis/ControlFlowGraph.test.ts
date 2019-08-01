import { InstructionSegment, SegmentType } from "../../data_formats/parsing/quest/bin";
import { assemble } from "../assembly";
import { BranchType, ControlFlowGraph } from "./ControlFlowGraph";

test("single instruction", () => {
    const im = to_instructions(`
        0:
            ret
    `);
    const cfg = ControlFlowGraph.create(im);

    expect(cfg.blocks.length).toBe(1);

    expect(cfg.blocks[0].start).toBe(0);
    expect(cfg.blocks[0].end).toBe(1);
    expect(cfg.blocks[0].branch_type).toBe(BranchType.Return);
    expect(cfg.blocks[0].from.length).toBe(0);
    expect(cfg.blocks[0].to.length).toBe(0);
    expect(cfg.blocks[0].branch_labels.length).toBe(0);
});

test("single unconditional jump", () => {
    const im = to_instructions(`
        0:
            jmp 1
        1:
            ret
    `);
    const cfg = ControlFlowGraph.create(im);

    expect(cfg.blocks.length).toBe(2);

    expect(cfg.blocks[0].start).toBe(0);
    expect(cfg.blocks[0].end).toBe(1);
    expect(cfg.blocks[0].branch_type).toBe(BranchType.Jump);
    expect(cfg.blocks[0].from.length).toBe(0);
    expect(cfg.blocks[0].to.length).toBe(1);
    expect(cfg.blocks[0].branch_labels.length).toBe(1);

    expect(cfg.blocks[1].start).toBe(0);
    expect(cfg.blocks[1].end).toBe(1);
    expect(cfg.blocks[1].branch_type).toBe(BranchType.Return);
    expect(cfg.blocks[1].from.length).toBe(1);
    expect(cfg.blocks[1].to.length).toBe(0);
    expect(cfg.blocks[1].branch_labels.length).toBe(0);
});

test("single conditional jump", () => {
    const im = to_instructions(`
        0:
            jmp_= r1, r2, 1
            ret
        1:
            ret
    `);
    const cfg = ControlFlowGraph.create(im);

    expect(cfg.blocks.length).toBe(3);

    expect(cfg.blocks[0].start).toBe(0);
    expect(cfg.blocks[0].end).toBe(1);
    expect(cfg.blocks[0].branch_type).toBe(BranchType.ConditionalJump);
    expect(cfg.blocks[0].from.length).toBe(0);
    expect(cfg.blocks[0].to.length).toBe(2);
    expect(cfg.blocks[0].branch_labels.length).toBe(1);

    expect(cfg.blocks[1].start).toBe(1);
    expect(cfg.blocks[1].end).toBe(2);
    expect(cfg.blocks[1].branch_type).toBe(BranchType.Return);
    expect(cfg.blocks[1].from.length).toBe(1);
    expect(cfg.blocks[1].to.length).toBe(0);
    expect(cfg.blocks[1].branch_labels.length).toBe(0);

    expect(cfg.blocks[2].start).toBe(0);
    expect(cfg.blocks[2].end).toBe(1);
    expect(cfg.blocks[2].branch_type).toBe(BranchType.Return);
    expect(cfg.blocks[2].from.length).toBe(1);
    expect(cfg.blocks[2].to.length).toBe(0);
    expect(cfg.blocks[2].branch_labels.length).toBe(0);
});

test("single call", () => {
    const im = to_instructions(`
        0:
            call 1
            ret
        1:
            ret
    `);
    const cfg = ControlFlowGraph.create(im);

    expect(cfg.blocks.length).toBe(3);

    expect(cfg.blocks[0].start).toBe(0);
    expect(cfg.blocks[0].end).toBe(1);
    expect(cfg.blocks[0].branch_type).toBe(BranchType.Call);
    expect(cfg.blocks[0].from.length).toBe(0);
    expect(cfg.blocks[0].to.length).toBe(1);
    expect(cfg.blocks[0].branch_labels.length).toBe(1);

    expect(cfg.blocks[1].start).toBe(1);
    expect(cfg.blocks[1].end).toBe(2);
    expect(cfg.blocks[1].branch_type).toBe(BranchType.Return);
    expect(cfg.blocks[1].from.length).toBe(1);
    expect(cfg.blocks[1].to.length).toBe(0);
    expect(cfg.blocks[1].branch_labels.length).toBe(0);

    expect(cfg.blocks[2].start).toBe(0);
    expect(cfg.blocks[2].end).toBe(1);
    expect(cfg.blocks[2].branch_type).toBe(BranchType.Return);
    expect(cfg.blocks[2].from.length).toBe(1);
    expect(cfg.blocks[2].to.length).toBe(1);
    expect(cfg.blocks[2].branch_labels.length).toBe(0);
});

test("conditional jump with fall-through", () => {
    const im = to_instructions(`
        0:
            jmp_> r1, r2, 1
            nop
        1:
            nop
            ret
    `);
    const cfg = ControlFlowGraph.create(im);

    expect(cfg.blocks.length).toBe(3);

    expect(cfg.blocks[0].start).toBe(0);
    expect(cfg.blocks[0].end).toBe(1);
    expect(cfg.blocks[0].branch_type).toBe(BranchType.ConditionalJump);
    expect(cfg.blocks[0].from.length).toBe(0);
    expect(cfg.blocks[0].to.length).toBe(2);
    expect(cfg.blocks[0].branch_labels.length).toBe(1);

    expect(cfg.blocks[1].start).toBe(1);
    expect(cfg.blocks[1].end).toBe(2);
    expect(cfg.blocks[1].branch_type).toBe(BranchType.None);
    expect(cfg.blocks[1].from.length).toBe(1);
    expect(cfg.blocks[1].to.length).toBe(1);
    expect(cfg.blocks[1].branch_labels.length).toBe(0);

    expect(cfg.blocks[2].start).toBe(0);
    expect(cfg.blocks[2].end).toBe(2);
    expect(cfg.blocks[2].branch_type).toBe(BranchType.Return);
    expect(cfg.blocks[2].from.length).toBe(2);
    expect(cfg.blocks[2].to.length).toBe(0);
    expect(cfg.blocks[2].branch_labels.length).toBe(0);
});

function to_instructions(assembly: string): InstructionSegment[] {
    const { object_code, warnings, errors } = assemble(assembly.split("\n"));

    expect(warnings).toEqual([]);
    expect(errors).toEqual([]);

    return object_code.filter(
        segment => segment.type === SegmentType.Instructions
    ) as InstructionSegment[];
}
