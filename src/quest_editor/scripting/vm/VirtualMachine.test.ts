/**
 * @jest-environment jsdom
 */

import { ExecutionResult, VirtualMachine } from "./VirtualMachine";
import { to_instructions } from "../../../../test/src/utils";
import { Segment } from "../../../core/data_formats/asm/instructions";
import { Random } from "./Random";
import { Episode } from "../../../core/data_formats/parsing/quest/Episode";
import { DefaultVirtualMachineIO } from "./io";

test("integer arithmetic opcodes", () => {
    class TestIO extends DefaultVirtualMachineIO {
        error = jest.fn((err: Error) => {
            throw err;
        });
    }

    const result_reg = 100;
    const vm = new VirtualMachine(new TestIO());

    function compute_arithmetic(obj_code: Segment[]): number {
        vm.load_object_code(obj_code, Episode.I);
        vm.start_thread(0);
        expect(vm.execute()).toBe(ExecutionResult.Suspended);
        return vm.get_register_signed(result_reg);
    }

    function compute_arithmetic_with_register(opcode: string, val1: number, val2: number): number {
        const other_reg = result_reg + 1;

        const obj_code = to_instructions(`
            0:
                leti r${result_reg}, ${val1}
                leti r${other_reg}, ${val2}
                ${opcode} r${result_reg}, r${other_reg}
                ret`);

        return compute_arithmetic(obj_code);
    }

    function compute_arithmetic_with_literal(opcode: string, val1: number, val2: number): number {
        const obj_code = to_instructions(`
            0:
                leti r${result_reg}, ${val1}
                ${opcode} r${result_reg}, ${val2}
                ret`);

        return compute_arithmetic(obj_code);
    }

    // opcodes that use two register
    expect(compute_arithmetic_with_register("add", 143, 656)).toBe(799);
    expect(compute_arithmetic_with_register("sub", 4390, 11670)).toBe(-7280);
    expect(compute_arithmetic_with_register("mul", 5356, 3)).toBe(16068);
    expect(compute_arithmetic_with_register("div", 131, 130)).toBe(1);
    expect(compute_arithmetic_with_register("mod", 5555555, 2)).toBe(1);

    // opcodes that use one register and one literal
    expect(compute_arithmetic_with_literal("addi", -3000, 5885858)).toBe(5882858);
    expect(compute_arithmetic_with_literal("subi", 1, -1)).toBe(2);
    expect(compute_arithmetic_with_literal("muli", 8008135, 0)).toBe(0);
    expect(compute_arithmetic_with_literal("divi", 500, 100)).toBe(5);
    expect(compute_arithmetic_with_literal("modi", 5959, 6969)).toBe(5959);

    // special cases:
    // integer overflow
    expect(compute_arithmetic_with_register("add", 2147483647, 1)).toBe(-2147483648);
    // under
    expect(compute_arithmetic_with_register("sub", -2147483648, 4444)).toBe(2147479204);
    // division by zero
    expect(() => compute_arithmetic_with_register("div", 1, 0)).toThrow();
});

// TODO: add more fp tests
test("floating point arithmetic opcodes", () => {
    class TestIO extends DefaultVirtualMachineIO {
        error = jest.fn((err: Error) => {
            throw err;
        });
    }

    const precision = 9;
    const obj_code = to_instructions(`
        0:
            fleti r100, 0.3
            fsubi r100, 0.2
            fsubi r100, 0.1
            fleti r101, 1.0
            fdiv r101, r100
            ret
    `);

    const vm = new VirtualMachine(new TestIO());
    vm.load_object_code(obj_code, Episode.I);
    vm.start_thread(0);

    expect(vm.execute()).toBe(ExecutionResult.Suspended);
    expect(vm.get_register_float(100)).toBeCloseTo(7.4505806e-9, precision);
    expect(vm.get_register_float(101)).toBeCloseTo(134217728, precision);
});

test("basic window_msg output", () => {
    const messages = ["foo", "bar", "buz"];
    const segments = to_instructions(
        `
        .code
        0:
            arg_pushs "${messages[0]}"
            window_msg
            arg_pushs "${messages[1]}"
            add_msg
            arg_pushs "${messages[2]}"
            add_msg
            winend
            ret
    `,
        true,
    );

    const messages_added: string[] = [];

    class TestIO extends DefaultVirtualMachineIO {
        window_msg = jest.fn((msg: string) => {
            messages_added.push(msg);
        });

        add_msg = jest.fn((msg: string) => {
            messages_added.push(msg);
        });

        winend = jest.fn(() => {}); // eslint-disable-line

        error = jest.fn((err: Error) => {
            throw err;
        });
    }

    const io = new TestIO();
    const vm = new VirtualMachine(io);

    vm.load_object_code(segments, Episode.I);
    vm.start_thread(0);

    const exec_results: ExecutionResult[] = [];
    let last_result: ExecutionResult;
    do {
        last_result = vm.execute();
        exec_results.push(last_result);
    } while (last_result === ExecutionResult.WaitingInput);

    expect(exec_results).toEqual([
        ExecutionResult.WaitingInput,
        ExecutionResult.WaitingInput,
        ExecutionResult.WaitingInput,
        ExecutionResult.Suspended,
    ]);

    expect(messages_added).toEqual(messages);

    expect(io.window_msg).toBeCalledTimes(1);
    expect(io.add_msg).toBeCalledTimes(2);
    expect(io.winend).toBeCalledTimes(1);
    expect(io.error).toBeCalledTimes(0);
});

test("opcode get_random", () => {
    const obj_code = to_instructions(`
    .code
    0:
        leti r100, 0
        leti r101, 65535
        get_random r100, r102
        get_random r100, r103
        get_random r100, r104
        get_random r100, r105
        get_random r100, r106
        get_random r100, r107
        get_random r100, r108
        ret
    `);

    const vm = new VirtualMachine(undefined, new Random(123));
    vm.load_object_code(obj_code, Episode.I);
    vm.start_thread(0);

    // test correct get_random sequence
    expect(vm.execute()).toBe(ExecutionResult.Suspended);
    expect(vm.get_register_unsigned(102)).toBe(879);
    expect(vm.get_register_unsigned(103)).toBe(38105);
    expect(vm.get_register_unsigned(104)).toBe(46149);
    expect(vm.get_register_unsigned(105)).toBe(26207);
    expect(vm.get_register_unsigned(106)).toBe(64725);
    expect(vm.get_register_unsigned(107)).toBe(6529);
    expect(vm.get_register_unsigned(108)).toBe(61497);
});

test("opcode list", () => {
    const list_items = ["a", "b", "c", "d"];
    const list_text = list_items.join("\\n");

    class TestIO extends DefaultVirtualMachineIO {
        constructor() {
            super();
        }

        list = jest.fn((items: string[]) => {
            expect(items).toEqual(list_items);
        });
    }

    const select_idx = 2;
    const result_reg = 100;
    const obj_code = to_instructions(`
    .code
    0:
        list r${result_reg}, "${list_text}"
        ret
    `);

    const vm = new VirtualMachine(new TestIO());
    vm.load_object_code(obj_code, Episode.I);
    vm.start_thread(0);

    expect(vm.execute()).toBe(ExecutionResult.WaitingSelection);
    vm.list_select(select_idx);
    expect(vm.get_register_unsigned(result_reg)).toBe(select_idx);
});
