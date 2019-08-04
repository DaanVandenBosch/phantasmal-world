import { Arg, Segment, SegmentType } from "./instructions";
import {
    Param,
    StackInteraction,
    TYPE_STRING,
    TYPE_I_LABEL_VAR,
    TYPE_REG_REF_VAR,
    TYPE_REG_REF,
    RegTupRefType,
} from "./opcodes";

/**
 * @param manual_stack If true, will output stack management instructions (argpush variants). Otherwise the arguments of stack management instructions will be output as arguments to the instruction that pops them from the stack.
 */
export function disassemble(object_code: Segment[], manual_stack: boolean = false): string[] {
    const lines: string[] = [];
    const stack: Arg[] = [];
    let code_block: boolean | undefined;

    for (const segment of object_code) {
        if (segment.type === SegmentType.Data) {
            if (code_block !== false) {
                code_block = false;

                if (lines.length) {
                    lines.push("");
                }

                lines.push(".data", "");
            }
        } else {
            if (code_block !== true) {
                code_block = true;

                if (lines.length) {
                    lines.push("");
                }

                lines.push(".code", "");
            }
        }

        for (const label of segment.labels) {
            lines.push(`${label}:`);
        }

        if (segment.type === SegmentType.Data) {
            const bytes = new Uint8Array(segment.data);
            let line = "    ";

            for (let i = 0; i < bytes.length; i++) {
                line += "0x" + bytes[i].toString(16).padStart(2, "0");

                if (i % 16 === 15) {
                    lines.push(line);
                    line = "    ";
                } else if (i < bytes.length - 1) {
                    line += " ";
                }
            }

            if (line.length > 4) {
                lines.push(line);
            }
        } else if (segment.type === SegmentType.String) {
            lines.push("    " + segment.value);
        } else {
            for (const instruction of segment.instructions) {
                if (!manual_stack && instruction.opcode.stack === StackInteraction.Push) {
                    stack.push(...instruction.args);
                } else {
                    let args: string[] = [];

                    if (instruction.opcode.stack === StackInteraction.Pop) {
                        if (!manual_stack) {
                            args = args_to_strings(
                                instruction.opcode.params,
                                stack.splice(
                                    Math.max(0, stack.length - instruction.opcode.params.length),
                                    instruction.opcode.params.length
                                )
                            );
                        }
                    } else {
                        args = args_to_strings(instruction.opcode.params, instruction.args);
                    }

                    lines.push(
                        "    " +
                            instruction.opcode.mnemonic +
                            (args.length ? " " + args.join(", ") : "")
                    );
                }
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
            case TYPE_I_LABEL_VAR:
                for (; i < args.length; i++) {
                    arg_strings.push(args[i].value.toString());
                }
                break;
            case TYPE_REG_REF_VAR:
                for (; i < args.length; i++) {
                    arg_strings.push("r" + args[i].value);
                }
                break;
            case TYPE_REG_REF:
                arg_strings.push("r" + arg.value);
                break;
            case TYPE_STRING:
                arg_strings.push(JSON.stringify(arg.value));
                break;
            default:
                if (type instanceof RegTupRefType) {
                    arg_strings.push("r" + arg.value);
                } else {
                    arg_strings.push(arg.value.toString());
                }
                break;
        }
    }

    return arg_strings;
}
