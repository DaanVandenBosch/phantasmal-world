import { assemble } from "../assembly";
import { InstructionSegment, SegmentType } from "../instructions";
import { Opcode } from "../opcodes";
import { ControlFlowGraph } from "./ControlFlowGraph";
import {
    MAX_REGISTER_VALUE,
    MIN_REGISTER_VALUE,
    register_value,
    REGISTER_VALUES,
} from "./register_value";

test(`trivial case`, () => {
    const im = to_instructions(`
        0:
            ret
    `);
    const cfg = ControlFlowGraph.create(im);
    const values = register_value(cfg, im[0].instructions[0], 6);

    expect(values.size()).toBe(0);
});

test(`single assignment`, () => {
    const im = to_instructions(`
        0:
            leti r6, 1337
            ret
    `);
    const cfg = ControlFlowGraph.create(im);
    const values = register_value(cfg, im[0].instructions[1], 6);

    expect(values.size()).toBe(1);
    expect(values.get(0)).toBe(1337);
});

test(`two code paths`, () => {
    const im = to_instructions(`
        0:
            jmp_> r1, r2, 1
            leti r10, 111
            jmp 2
        1:
            leti r10, 222
        2:
            ret
    `);
    const cfg = ControlFlowGraph.create(im);
    const values = register_value(cfg, im[2].instructions[0], 10);

    expect(values.size()).toBe(2);
    expect(values.get(0)).toBe(111);
    expect(values.get(1)).toBe(222);
});

test(`loop`, () => {
    const im = to_instructions(`
        0:
            addi r10, 5
            jmpi_< r10, 500, 0
            ret
    `);
    const cfg = ControlFlowGraph.create(im);
    const values = register_value(cfg, im[0].instructions[2], 10);

    expect(values.size()).toBe(REGISTER_VALUES);
});

test(`leta and leto`, () => {
    const im = to_instructions(`
        0:
            leta r0, r100
            leto r1, 100
            ret
    `);
    const cfg = ControlFlowGraph.create(im);
    const r0 = register_value(cfg, im[0].instructions[2], 0);

    expect(r0.size()).toBe(REGISTER_VALUES);
    expect(r0.min()).toBe(MIN_REGISTER_VALUE);
    expect(r0.max()).toBe(MAX_REGISTER_VALUE);

    const r1 = register_value(cfg, im[0].instructions[2], 1);

    expect(r1.size()).toBe(REGISTER_VALUES);
    expect(r1.min()).toBe(MIN_REGISTER_VALUE);
    expect(r1.max()).toBe(MAX_REGISTER_VALUE);
});

test(`rev`, () => {
    const im = to_instructions(`
        0:
            leti r0, 10
            leti r1, 50
            get_random r0, r10
            rev r10
            leti r0, -10
            leti r1, 50
            get_random r0, r10
            rev r10
            leti r10, 0
            rev r10
            ret
    `);
    const cfg = ControlFlowGraph.create(im);
    const v0 = register_value(cfg, im[0].instructions[4], 10);

    expect(v0.size()).toBe(1);
    expect(v0.get(0)).toBe(0);

    const v1 = register_value(cfg, im[0].instructions[8], 10);

    expect(v1.size()).toBe(2);
    expect(v1.to_array()).toEqual([0, 1]);

    const v2 = register_value(cfg, im[0].instructions[10], 10);

    expect(v2.size()).toBe(1);
    expect(v2.get(0)).toBe(1);
});

/**
 * Test an instruction taking a register and an integer.
 * The instruction will be called with arguments r99, 15. r99 will be set to 10 or 20.
 */
function test_branched(opcode: Opcode, ...expected: number[]): void {
    test(opcode.mnemonic, () => {
        const im = to_instructions(`
        0:
            leti r99, 10
            jmpi_= r0, 100, 1
            leti r99, 20
        1:
            ${opcode.mnemonic} r99, 15
            ret
    `);
        const cfg = ControlFlowGraph.create(im);
        const values = register_value(cfg, im[1].instructions[1], 99);

        expect(values.size()).toBe(expected.length);
        expect(values.to_array()).toEqual(expected);
    });
}

test_branched(Opcode.ADDI, 25, 35);
test_branched(Opcode.SUBI, -5, 5);
test_branched(Opcode.MULI, 150, 300);
test_branched(Opcode.DIVI, 0, 1);

test(`get_random`, () => {
    const im = to_instructions(`
        0:
            leti r0, 20
            leti r1, 20
            get_random r0, r10
            leti r1, 19
            get_random r0, r10
            leti r1, 25
            get_random r0, r10
            ret
    `);
    const cfg = ControlFlowGraph.create(im);
    const v0 = register_value(cfg, im[0].instructions[3], 10);

    expect(v0.size()).toBe(1);
    expect(v0.get(0)).toBe(20);

    const v1 = register_value(cfg, im[0].instructions[5], 10);

    expect(v1.size()).toBe(1);
    expect(v1.get(0)).toBe(20);

    const v2 = register_value(cfg, im[0].instructions[7], 10);

    expect(v2.size()).toBe(5);
    expect(v2.to_array()).toEqual([20, 21, 22, 23, 24]);
});

function to_instructions(assembly: string): InstructionSegment[] {
    const { object_code, warnings, errors } = assemble(assembly.split("\n"));

    expect(warnings).toEqual([]);
    expect(errors).toEqual([]);

    return object_code.filter(
        segment => segment.type === SegmentType.Instructions,
    ) as InstructionSegment[];
}
