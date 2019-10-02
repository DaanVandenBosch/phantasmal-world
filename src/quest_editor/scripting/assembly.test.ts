import { assemble } from "./assembly";
import { InstructionSegment, SegmentType } from "./instructions";
import {
    OP_ARG_PUSHB,
    OP_ARG_PUSHL,
    OP_ARG_PUSHR,
    OP_ARG_PUSHW,
    OP_BB_MAP_DESIGNATE,
    OP_RET,
    OP_SET_EPISODE,
    OP_SET_FLOOR_HANDLER,
    OP_SET_MAINWARP,
} from "./opcodes";

test("basic script", () => {
    const { object_code, warnings, errors } = assemble(
        `
    0:   set_episode 0
         bb_map_designate 1, 2, 3, 4
         set_floor_handler 0, 150
         set_floor_handler 1, 151
         ret
    150: set_mainwarp 1
         ret
    151: ret
    `.split("\n"),
    );

    expect(warnings).toEqual([]);
    expect(errors).toEqual([]);

    expect(object_code.length).toBe(3);

    const segment_0 = object_code[0] as InstructionSegment;

    expect(segment_0.type).toBe(SegmentType.Instructions);
    expect(segment_0.instructions.length).toBe(9);

    expect(segment_0.instructions[0].opcode).toBe(OP_SET_EPISODE);
    expect(segment_0.instructions[0].args).toEqual([
        { value: 0, size: 4, asm: { line_no: 2, col: 22, len: 1 } },
    ]);

    expect(segment_0.instructions[1].opcode).toBe(OP_BB_MAP_DESIGNATE);
    expect(segment_0.instructions[1].args).toEqual([
        { value: 1, size: 1, asm: { line_no: 3, col: 27, len: 1 } },
        { value: 2, size: 2, asm: { line_no: 3, col: 30, len: 1 } },
        { value: 3, size: 1, asm: { line_no: 3, col: 33, len: 1 } },
        { value: 4, size: 1, asm: { line_no: 3, col: 36, len: 1 } },
    ]);

    expect(segment_0.instructions[2].opcode).toBe(OP_ARG_PUSHL);
    expect(segment_0.instructions[2].args).toEqual([
        { value: 0, size: 4, asm: { line_no: 4, col: 28, len: 1 } },
    ]);
    expect(segment_0.instructions[3].opcode).toBe(OP_ARG_PUSHW);
    expect(segment_0.instructions[3].args).toEqual([
        { value: 150, size: 2, asm: { line_no: 4, col: 31, len: 3 } },
    ]);
    expect(segment_0.instructions[4].opcode).toBe(OP_SET_FLOOR_HANDLER);
    expect(segment_0.instructions[4].args).toEqual([]);

    expect(segment_0.instructions[5].opcode).toBe(OP_ARG_PUSHL);
    expect(segment_0.instructions[5].args).toEqual([
        { value: 1, size: 4, asm: { line_no: 5, col: 28, len: 1 } },
    ]);
    expect(segment_0.instructions[6].opcode).toBe(OP_ARG_PUSHW);
    expect(segment_0.instructions[6].args).toEqual([
        { value: 151, size: 2, asm: { line_no: 5, col: 31, len: 3 } },
    ]);
    expect(segment_0.instructions[7].opcode).toBe(OP_SET_FLOOR_HANDLER);
    expect(segment_0.instructions[7].args).toEqual([]);

    expect(segment_0.instructions[8].opcode).toBe(OP_RET);
    expect(segment_0.instructions[8].args).toEqual([]);

    const segment_1 = object_code[1] as InstructionSegment;

    expect(segment_1.type).toBe(SegmentType.Instructions);
    expect(segment_1.instructions.length).toBe(3);

    expect(segment_1.instructions[0].opcode).toBe(OP_ARG_PUSHL);
    expect(segment_1.instructions[0].args).toEqual([
        { value: 1, size: 4, asm: { line_no: 7, col: 23, len: 1 } },
    ]);
    expect(segment_1.instructions[1].opcode).toBe(OP_SET_MAINWARP);
    expect(segment_1.instructions[1].args).toEqual([]);

    expect(segment_1.instructions[2].opcode).toBe(OP_RET);
    expect(segment_1.instructions[2].args).toEqual([]);

    const segment_2 = object_code[2] as InstructionSegment;

    expect(segment_2.type).toBe(SegmentType.Instructions);
    expect(segment_2.instructions.length).toBe(1);

    expect(segment_2.instructions[0].opcode).toBe(OP_RET);
    expect(segment_2.instructions[0].args).toEqual([]);
});

test("pass the value of a register via the stack", () => {
    const { object_code, warnings, errors } = assemble(
        `
    0:
        leti r255, 7
        exit r255
        ret
    `.split("\n"),
    );

    expect(warnings).toEqual([]);
    expect(errors).toEqual([]);

    expect(object_code.length).toBe(1);

    const segment_0 = object_code[0] as InstructionSegment;

    expect(segment_0.type).toBe(SegmentType.Instructions);
    expect(segment_0.instructions.length).toBe(4);

    expect(segment_0.instructions[1].opcode).toBe(OP_ARG_PUSHR);
    expect(segment_0.instructions[1].args).toEqual([
        { value: 255, size: 1, asm: { line_no: 4, col: 14, len: 4 } },
    ]);
});

test("pass a register reference via the stack", () => {
    const { object_code, warnings, errors } = assemble(
        `
    0:
        p_dead_v3 r200, 3
        ret
    `.split("\n"),
    );

    expect(warnings).toEqual([]);
    expect(errors).toEqual([]);

    expect(object_code.length).toBe(1);

    const segment_0 = object_code[0] as InstructionSegment;

    expect(segment_0.type).toBe(SegmentType.Instructions);
    expect(segment_0.instructions.length).toBe(4);

    expect(segment_0.instructions[0].opcode).toBe(OP_ARG_PUSHB);
    expect(segment_0.instructions[0].args).toEqual([
        { value: 200, size: 1, asm: { line_no: 3, col: 19, len: 4 } },
    ]);

    expect(segment_0.instructions[1].opcode).toBe(OP_ARG_PUSHL);
    expect(segment_0.instructions[1].args).toEqual([
        { value: 3, size: 4, asm: { line_no: 3, col: 25, len: 1 } },
    ]);
});
