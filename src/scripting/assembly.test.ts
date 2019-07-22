import { assemble } from "./assembly";
import { Opcode } from "../data_formats/parsing/quest/bin";

test("", () => {
    const { instructions, labels, errors } = assemble(`
    0:   set_episode 0
         bb_map_designate 1, 2, 3, 4
         set_floor_handler 0, 150
         set_floor_handler 1, 151
         ret
    150: set_mainwarp 1
         ret
    151: ret
    `);

    expect(errors).toEqual([]);

    expect(instructions.length).toBe(13);

    expect(instructions[0].opcode).toBe(Opcode.set_episode);
    expect(instructions[0].args).toEqual([{ value: 0, size: 4 }]);

    expect(instructions[1].opcode).toBe(Opcode.bb_map_designate);
    expect(instructions[1].args).toEqual([
        { value: 1, size: 1 },
        { value: 2, size: 2 },
        { value: 3, size: 1 },
        { value: 4, size: 1 },
    ]);

    expect(instructions[2].opcode).toBe(Opcode.arg_pushl);
    expect(instructions[2].args).toEqual([{ value: 0, size: 4 }]);
    expect(instructions[3].opcode).toBe(Opcode.arg_pushw);
    expect(instructions[3].args).toEqual([{ value: 150, size: 2 }]);
    expect(instructions[4].opcode).toBe(Opcode.set_floor_handler);
    expect(instructions[4].args).toEqual([]);

    expect(instructions[5].opcode).toBe(Opcode.arg_pushl);
    expect(instructions[5].args).toEqual([{ value: 1, size: 4 }]);
    expect(instructions[6].opcode).toBe(Opcode.arg_pushw);
    expect(instructions[6].args).toEqual([{ value: 151, size: 2 }]);
    expect(instructions[7].opcode).toBe(Opcode.set_floor_handler);
    expect(instructions[7].args).toEqual([]);

    expect(instructions[8].opcode).toBe(Opcode.ret);
    expect(instructions[8].args).toEqual([]);

    expect(instructions[9].opcode).toBe(Opcode.arg_pushl);
    expect(instructions[9].args).toEqual([{ value: 1, size: 4 }]);
    expect(instructions[10].opcode).toBe(Opcode.set_mainwarp);
    expect(instructions[10].args).toEqual([]);

    expect(instructions[11].opcode).toBe(Opcode.ret);
    expect(instructions[11].args).toEqual([]);

    expect(instructions[12].opcode).toBe(Opcode.ret);
    expect(instructions[12].args).toEqual([]);

    expect(labels).toEqual(new Map([[0, 0], [150, 9], [151, 12]]));
});
