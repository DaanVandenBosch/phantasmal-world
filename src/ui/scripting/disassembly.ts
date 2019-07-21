import { Arg, Type } from "../../data_formats/parsing/quest/bin";
import { Quest } from "../../domain";

export function disassemble(quest: Quest, manual_stack: boolean = false): string {
    const lines: string[] = [];
    const index_to_label = [...quest.labels.entries()].reduce(
        (map, [l, i]) => map.set(i, l),
        new Map<number, number>()
    );

    const stack: Arg[] = [];

    for (let i = 0; i < quest.instructions.length; ++i) {
        const ins = quest.instructions[i];
        const label = index_to_label.get(i);

        if (!manual_stack && ins.opcode.push_stack) {
            stack.push(...ins.args);
        } else {
            let args: string[] = [];

            for (let j = 0; j < ins.opcode.params.length; j++) {
                const param_type = ins.opcode.params[j];
                const arg = ins.args[j];
                args.push(...arg_to_strings(param_type, arg));
            }

            if (!manual_stack) {
                for (let j = ins.opcode.stack_params.length - 1; j >= 0; j--) {
                    const param_type = ins.opcode.stack_params[j];
                    const arg = stack.pop();

                    if (!arg) {
                        break;
                    }

                    args.push(...arg_to_strings(param_type, arg));
                }
            }

            if (label != null) {
                lines.push(`${label}:`);
            }

            lines.push("    " + ins.opcode.mnemonic + (args.length ? " " + args.join(", ") : ""));
        }
    }

    return lines.join("\n");
}

function arg_to_strings(param_type: Type, arg: Arg): string[] {
    switch (param_type) {
        case Type.U8:
        case Type.U16:
        case Type.U32:
        case Type.I32:
        case Type.F32:
            return [arg.value.toString()];
        case Type.Register:
            return ["r" + arg.value];
        case Type.SwitchData:
        case Type.JumpData:
            return arg.value.map(String);
        case Type.String:
            return [JSON.stringify(arg.value)];
    }
}
