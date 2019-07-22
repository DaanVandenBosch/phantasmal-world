import { Arg, Param, Type } from "../../data_formats/parsing/quest/bin";
import { Quest } from "../../domain";

/**
 * @param manual_stack If true, will ouput stack management instructions (argpush variants). Otherwise stack management instructions will not be output and their arguments will be output as arguments to the instruction that pops them from the stack.
 */
export function disassemble(quest: Quest, manual_stack: boolean = false): string {
    const lines: string[] = [];
    const index_to_label = new Map([...quest.labels.entries()].map(([l, i]) => [i, l]));

    const stack: Arg[] = [];

    for (let i = 0; i < quest.instructions.length; ++i) {
        const ins = quest.instructions[i];
        const label = index_to_label.get(i);

        if (!manual_stack && ins.opcode.push_stack) {
            stack.push(...ins.args);
        } else {
            let args = args_to_strings(ins.opcode.params, ins.args);

            if (!manual_stack) {
                args.push(
                    ...args_to_strings(
                        ins.opcode.stack_params,
                        stack.splice(
                            Math.max(0, stack.length - ins.opcode.stack_params.length),
                            ins.opcode.stack_params.length
                        )
                    )
                );
            }

            if (label != null) {
                lines.push(`${label}:`);
            }

            lines.push("    " + ins.opcode.mnemonic + (args.length ? " " + args.join(", ") : ""));
        }
    }

    // Ensure newline.
    if (lines.length) {
        lines.push("");
    }

    return lines.join("\n");
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
            case Type.U16Var:
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
