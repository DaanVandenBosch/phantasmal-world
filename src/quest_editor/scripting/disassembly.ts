import { reinterpret_i32_as_f32 } from "../../core/primitive_conversion";
import { Arg, Segment, SegmentType } from "../../core/data_formats/asm/instructions";
import {
    AnyType,
    Kind,
    OP_VA_END,
    OP_VA_START,
    Param,
    StackInteraction,
} from "../../core/data_formats/asm/opcodes";
import { LogManager } from "../../core/Logger";
import { number_to_hex_string } from "../../core/util";

const logger = LogManager.get("quest_editor/scripting/disassembly");

type ArgWithType = Arg & {
    /**
     * Type inferred from the specific instruction used to push this argument onto the stack.
     */
    type: AnyType;
};

/**
 * @param object_code - The object code to disassemble.
 * @param manual_stack - If true, will output stack management instructions (argpush variants). Otherwise the arguments of stack management instructions will be output as arguments to the instruction that pops them from the stack.
 */
export function disassemble(object_code: readonly Segment[], manual_stack = false): string[] {
    logger.trace("disassemble start");

    const lines: string[] = [];
    const stack: ArgWithType[] = [];
    let section_type: SegmentType | undefined = undefined;

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
                line += "0x" + number_to_hex_string(bytes[i], 2);

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
            lines.push("    " + string_segment_to_string(segment.value));
        } else {
            // SegmentType.Instructions
            let in_va_list = false;

            for (const instruction of segment.instructions) {
                if (instruction.opcode.code === OP_VA_START.code) {
                    in_va_list = true;
                } else if (instruction.opcode.code === OP_VA_END.code) {
                    in_va_list = false;
                }

                if (
                    !manual_stack &&
                    !in_va_list &&
                    instruction.opcode.stack === StackInteraction.Push
                ) {
                    stack.push(...add_type_to_args(instruction.opcode.params, instruction.args));
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
                        args = args_to_strings(
                            instruction.opcode.params,
                            add_type_to_args(instruction.opcode.params, instruction.args),
                            false,
                        );
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

    logger.trace(`disassemble end, line count: ${lines.length}`);
    return lines;
}

function add_type_to_args(params: readonly Param[], args: readonly Arg[]): ArgWithType[] {
    const args_with_type: ArgWithType[] = [];
    const len = Math.min(params.length, args.length);

    for (let i = 0; i < len; i++) {
        args_with_type.push({ ...args[i], type: params[i].type });
    }

    // Deal with varargs.
    const last_param = params[params.length - 1];

    if (
        last_param &&
        (last_param.type.kind === Kind.ILabelVar || last_param.type.kind === Kind.RegRefVar)
    ) {
        const len = args.length;

        for (let i = args_with_type.length; i < len; i++) {
            args_with_type.push({ ...args[i], type: last_param.type });
        }
    }

    return args_with_type;
}

function args_to_strings(params: readonly Param[], args: ArgWithType[], stack: boolean): string[] {
    const arg_strings: string[] = [];

    for (let i = 0; i < params.length; i++) {
        const type = params[i].type;
        const arg = args[i];

        if (arg == undefined) {
            arg_strings.push("");
            continue;
        }

        if (arg.type.kind === Kind.RegTupRef) {
            arg_strings.push("r" + arg.value);
        } else {
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
    }

    return arg_strings;
}

function string_segment_to_string(str: string): string {
    return JSON.stringify(str.replace(/<cr>/g, "\n"));
}
