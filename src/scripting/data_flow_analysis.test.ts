import { InstructionSegment, SegmentType } from "../data_formats/parsing/quest/bin";
import { assemble } from "./assembly";
import { create_control_flow_graph, BranchType } from "./data_flow_analysis";

test("single instruction", () => {
    const im = to_instructions(`
        0:
            ret
    `);
    const cfg = create_control_flow_graph(im);

    expect(cfg.nodes.length).toBe(1);

    expect(cfg.nodes[0].start).toBe(0);
    expect(cfg.nodes[0].end).toBe(1);
    expect(cfg.nodes[0].branch_type).toBe(BranchType.Return);
    expect(cfg.nodes[0].from.length).toBe(0);
    expect(cfg.nodes[0].to.length).toBe(0);
    expect(cfg.nodes[0].branch_labels.length).toBe(0);
});

test("single unconditional jump", () => {
    const im = to_instructions(`
        0:
            jmp 1
        1:
            ret
    `);
    const cfg = create_control_flow_graph(im);

    expect(cfg.nodes.length).toBe(2);

    expect(cfg.nodes[0].start).toBe(0);
    expect(cfg.nodes[0].end).toBe(1);
    expect(cfg.nodes[0].branch_type).toBe(BranchType.Jump);
    expect(cfg.nodes[0].from.length).toBe(0);
    expect(cfg.nodes[0].to.length).toBe(1);
    expect(cfg.nodes[0].branch_labels.length).toBe(1);

    expect(cfg.nodes[1].start).toBe(0);
    expect(cfg.nodes[1].end).toBe(1);
    expect(cfg.nodes[1].branch_type).toBe(BranchType.Return);
    expect(cfg.nodes[1].from.length).toBe(1);
    expect(cfg.nodes[1].to.length).toBe(0);
    expect(cfg.nodes[1].branch_labels.length).toBe(0);
});

test("single conditional jump", () => {
    const im = to_instructions(`
        0:
            jmp_= r1, r2, 1
            ret
        1:
            ret
    `);
    const cfg = create_control_flow_graph(im);

    expect(cfg.nodes.length).toBe(3);

    expect(cfg.nodes[0].start).toBe(0);
    expect(cfg.nodes[0].end).toBe(1);
    expect(cfg.nodes[0].branch_type).toBe(BranchType.ConditionalJump);
    expect(cfg.nodes[0].from.length).toBe(0);
    expect(cfg.nodes[0].to.length).toBe(2);
    expect(cfg.nodes[0].branch_labels.length).toBe(1);

    expect(cfg.nodes[1].start).toBe(1);
    expect(cfg.nodes[1].end).toBe(2);
    expect(cfg.nodes[1].branch_type).toBe(BranchType.Return);
    expect(cfg.nodes[1].from.length).toBe(1);
    expect(cfg.nodes[1].to.length).toBe(0);
    expect(cfg.nodes[1].branch_labels.length).toBe(0);

    expect(cfg.nodes[2].start).toBe(0);
    expect(cfg.nodes[2].end).toBe(1);
    expect(cfg.nodes[2].branch_type).toBe(BranchType.Return);
    expect(cfg.nodes[2].from.length).toBe(1);
    expect(cfg.nodes[2].to.length).toBe(0);
    expect(cfg.nodes[2].branch_labels.length).toBe(0);
});

test("single call", () => {
    const im = to_instructions(`
        0:
            call 1
            ret
        1:
            ret
    `);
    const cfg = create_control_flow_graph(im);

    expect(cfg.nodes.length).toBe(3);

    expect(cfg.nodes[0].start).toBe(0);
    expect(cfg.nodes[0].end).toBe(1);
    expect(cfg.nodes[0].branch_type).toBe(BranchType.Call);
    expect(cfg.nodes[0].from.length).toBe(0);
    expect(cfg.nodes[0].to.length).toBe(1);
    expect(cfg.nodes[0].branch_labels.length).toBe(1);

    expect(cfg.nodes[1].start).toBe(1);
    expect(cfg.nodes[1].end).toBe(2);
    expect(cfg.nodes[1].branch_type).toBe(BranchType.Return);
    expect(cfg.nodes[1].from.length).toBe(1);
    expect(cfg.nodes[1].to.length).toBe(0);
    expect(cfg.nodes[1].branch_labels.length).toBe(0);

    expect(cfg.nodes[2].start).toBe(0);
    expect(cfg.nodes[2].end).toBe(1);
    expect(cfg.nodes[2].branch_type).toBe(BranchType.Return);
    expect(cfg.nodes[2].from.length).toBe(1);
    expect(cfg.nodes[2].to.length).toBe(1);
    expect(cfg.nodes[2].branch_labels.length).toBe(0);
});

test("conditional branch with fall-through", () => {
    const im = to_instructions(`
        0:
            jmp_> r1, r2, 1
            nop
        1:
            nop
            ret
    `);
    const cfg = create_control_flow_graph(im);

    expect(cfg.nodes.length).toBe(3);

    expect(cfg.nodes[0].start).toBe(0);
    expect(cfg.nodes[0].end).toBe(1);
    expect(cfg.nodes[0].branch_type).toBe(BranchType.ConditionalJump);
    expect(cfg.nodes[0].from.length).toBe(0);
    expect(cfg.nodes[0].to.length).toBe(2);
    expect(cfg.nodes[0].branch_labels.length).toBe(1);

    expect(cfg.nodes[1].start).toBe(1);
    expect(cfg.nodes[1].end).toBe(2);
    expect(cfg.nodes[1].branch_type).toBe(BranchType.None);
    expect(cfg.nodes[1].from.length).toBe(1);
    expect(cfg.nodes[1].to.length).toBe(1);
    expect(cfg.nodes[1].branch_labels.length).toBe(0);

    expect(cfg.nodes[2].start).toBe(0);
    expect(cfg.nodes[2].end).toBe(2);
    expect(cfg.nodes[2].branch_type).toBe(BranchType.Return);
    expect(cfg.nodes[2].from.length).toBe(2);
    expect(cfg.nodes[2].to.length).toBe(0);
    expect(cfg.nodes[2].branch_labels.length).toBe(0);
});

function to_instructions(assembly: string): InstructionSegment[] {
    const { object_code, warnings, errors } = assemble(assembly.split("\n"));

    expect(warnings).toEqual([]);
    expect(errors).toEqual([]);

    return object_code.filter(
        segment => segment.type === SegmentType.Instructions
    ) as InstructionSegment[];
}
