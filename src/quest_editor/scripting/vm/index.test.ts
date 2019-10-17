import { VirtualMachine, ExecutionResult } from ".";
import { VMIOStub } from "./VMIOStub";
import { to_instructions } from "../../../../test/src/utils";

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
    `,
        true,
    );

    class TestIO extends VMIOStub {
        window_msg = jest.fn((msg: string) => {
            expect(msg).toBe(messages.shift());
        });

        add_msg = jest.fn((msg: string) => {
            expect(msg).toBe(messages.shift());
        });

        winend = jest.fn(() => {});

        error = jest.fn((err: Error, loc: any) => {
            throw err;
        });
    }

    const io = new TestIO();
    const vm = new VirtualMachine(io);

    vm.load_object_code(segments);
    vm.start_thread(0);

    const exec_results: ExecutionResult[] = [];
    let last_result: ExecutionResult;
    do {
        last_result = vm.execute();
        exec_results.push(last_result);
    } while (last_result !== ExecutionResult.Halted);

    expect(exec_results).toHaveLength(segments[0].instructions.length);
    expect(exec_results).toEqual(
        Array(segments[0].instructions.length - 1)
            .fill(ExecutionResult.Ok)
            .concat(ExecutionResult.Halted),
    );

    expect(io.window_msg).toBeCalledTimes(1);
    expect(io.add_msg).toBeCalledTimes(2);
    expect(io.winend).toBeCalledTimes(1);
    expect(io.error).toBeCalledTimes(0);
});
