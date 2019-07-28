import { Arg, Param, Segment, SegmentType, Type } from "../data_formats/parsing/quest/bin";

/**
 * @param manual_stack If true, will output stack management instructions (argpush variants). Otherwise the arguments of stack management instructions will be output as arguments to the instruction that pops them from the stack.
 */
export function disassemble(object_code: Segment[], manual_stack: boolean = false): string[] {
    const lines: string[] = [];
    const stack: Arg[] = [];

    for (const segment of object_code) {
        if (segment.type === SegmentType.Data) {
            continue;
        }

        if (segment.label !== -1) {
            lines.push(`${segment.label}:`);
        }

        for (const instruction of segment.instructions) {
            if (!manual_stack && instruction.opcode.push_stack) {
                stack.push(...instruction.args);
            } else {
                let args = args_to_strings(instruction.opcode.params, instruction.args);

                if (!manual_stack) {
                    args.push(
                        ...args_to_strings(
                            instruction.opcode.stack_params,
                            stack.splice(
                                Math.max(0, stack.length - instruction.opcode.stack_params.length),
                                instruction.opcode.stack_params.length
                            )
                        )
                    );
                }

                lines.push(
                    "    " +
                        instruction.opcode.mnemonic +
                        (args.length ? " " + args.join(", ") : "")
                );
            }
        }
    }

    // Ensure newline at the end.
    if (lines.length) {
        lines.push("");
    }

    return lines;
}

function args_to_strings(params: Param[], args: Arg[]): string[] {
    const arg_strings: string[] = [];

    for (let i = 0; i < params.length; i++) {
        const type = params[i].type;
        const arg = args[i];

        if (arg == null) {
            arg_strings.push("");
            continue;
        }

        switch (type) {
            case Type.U8Var:
            case Type.ILabelVar:
                for (; i < args.length; i++) {
                    arg_strings.push(args[i].value.toString());
                }

                break;
            case Type.Register:
                arg_strings.push("r" + arg.value);
                break;
            case Type.String:
                arg_strings.push(JSON.stringify(arg.value));
                break;
            default:
                arg_strings.push(arg.value.toString());
                break;
        }
    }

    return arg_strings;
}
