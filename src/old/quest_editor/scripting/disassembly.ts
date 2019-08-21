import { reinterpret_i32_as_f32 } from "../../core/primitive_conversion";
import { Arg, Segment, SegmentType } from "./instructions";
import { Kind, Param, StackInteraction } from "./opcodes";

/**
 * @param manual_stack If true, will output stack management instructions (argpush variants). Otherwise the arguments of stack management instructions will be output as arguments to the instruction that pops them from the stack.
 */
export function disassemble(object_code: Segment[], manual_stack: boolean = false): string[] {
    const lines: string[] = [];
    const stack: Arg[] = [];
    let section_type: SegmentType | undefined;

    for (const segment of object_code) {
        // Section marker.
        let section_marker!: string;

        switch (segment.type) {
            case SegmentType.Instructions:
                section_marker = ".code";
                break;
            case SegmentType.Data:
                section_marker = ".data";
                break;
            case SegmentType.String:
                section_marker = ".string";
                break;
        }

        if (section_type !== segment.type) {
            section_type = segment.type;

            if (lines.length) {
                lines.push("");
            }

            lines.push(section_marker, "");
        }

        // Labels.
        for (const label of segment.labels) {
            lines.push(`${label}:`);
        }

        // Code or data lines.
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
            lines.push("    " + JSON.stringify(segment.value));
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
                                    instruction.opcode.params.length,
                                ),
                                true,
                            );
                        }
                    } else {
                        args = args_to_strings(instruction.opcode.params, instruction.args, false);
                    }

                    lines.push(
                        "    " +
                            instruction.opcode.mnemonic +
                            (args.length ? " " + args.join(", ") : ""),
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

function args_to_strings(params: Param[], args: Arg[], stack: boolean): string[] {
    const arg_strings: string[] = [];

    for (let i = 0; i < params.length; i++) {
        const type = params[i].type;
        const arg = args[i];

        if (arg == null) {
            arg_strings.push("");
            continue;
        }

        switch (type.kind) {
            case Kind.Float:
                // Floats are pushed onto the stack as integers with arg_pushl.
                if (stack) {
                    arg_strings.push(reinterpret_i32_as_f32(arg.value).toString());
                } else {
                    arg_strings.push(arg.value.toString());
                }
                break;
            case Kind.ILabelVar:
                for (; i < args.length; i++) {
                    arg_strings.push(args[i].value.toString());
                }
                break;
            case Kind.RegRefVar:
                for (; i < args.length; i++) {
                    arg_strings.push("r" + args[i].value);
                }
                break;
            case Kind.RegRef:
            case Kind.RegTupRef:
                arg_strings.push("r" + arg.value);
                break;
            case Kind.String:
                arg_strings.push(JSON.stringify(arg.value));
                break;
            default:
                arg_strings.push(arg.value.toString());
                break;
        }
    }

    return arg_strings;
}
