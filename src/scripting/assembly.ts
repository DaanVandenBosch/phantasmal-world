import {
    Instruction,
    OPCODES_BY_MNEMONIC,
    Arg,
    Type,
    Opcode,
    Param,
} from "../data_formats/parsing/quest/bin";

type DisassemblyError = {
    line: number;
    col: number;
    length: number;
    description: string;
};

export function assemble(
    assembly: string,
    manual_stack: boolean = false
): {
    instructions: Instruction[];
    labels: Map<number, number>;
    errors: DisassemblyError[];
} {
    const errors: DisassemblyError[] = [];
    const instructions: Instruction[] = [];
    const labels = new Map<number, number>();

    let line = 1;

    for (const line_text of assembly.split("\n")) {
        const match = line_text.match(
            /^(?<lbl_ws>\s*)(?<lbl>[^\s]+?:)?(?<op_ws>\s*)(?<op>[a-z][a-z_=<>!]*)?(?<args>.*)$/
        );

        if (!match || !match.groups || (match.groups.lbl == null && match.groups.op == null)) {
            const left_trimmed = line_text.trimLeft();
            const trimmed = left_trimmed.trimRight();

            if (trimmed.length) {
                errors.push({
                    line,
                    col: line_text.length - left_trimmed.length,
                    length: trimmed.length,
                    description: "Expected label or instruction.",
                });
            }
        } else {
            const { lbl_ws, lbl, op_ws, op, args } = match.groups;

            if (lbl != null) {
                const label = parseInt(lbl.slice(0, -1), 10);

                if (!isFinite(label)) {
                    errors.push({
                        line,
                        col: lbl_ws.length,
                        length: lbl.length,
                        description: "Invalid label name.",
                    });
                } else if (labels.has(label)) {
                    errors.push({
                        line,
                        col: lbl_ws.length,
                        length: lbl.length - 1,
                        description: "Duplicate label.",
                    });
                } else {
                    labels.set(label, instructions.length);
                }
            }

            if (op != null) {
                const opcode = OPCODES_BY_MNEMONIC.get(op);

                if (!opcode) {
                    errors.push({
                        line,
                        col: lbl_ws.length + (lbl ? lbl.length : 0) + op_ws.length,
                        length: op.length,
                        description: "Unknown instruction.",
                    });
                } else {
                    const args_col =
                        lbl_ws.length +
                        (lbl ? lbl.length : 0) +
                        op_ws.length +
                        (op ? op.length : 0);

                    const arg_tokens: ArgToken[] = [];
                    const args_tokenization_ok = tokenize_args(args, args_col, arg_tokens);

                    const ins_args: Arg[] = [];

                    if (!args_tokenization_ok) {
                        const left_trimmed = args.trimLeft();
                        const trimmed = args.trimRight();

                        if (trimmed.trim().length) {
                            errors.push({
                                line,
                                col: args_col + args.length - left_trimmed.length,
                                length: trimmed.length,
                                description: "Instruction arguments expected.",
                            });
                        }
                    } else {
                        const param_count =
                            opcode.params.length + (manual_stack ? 0 : opcode.stack_params.length);

                        if (arg_tokens.length !== param_count) {
                            const left_trimmed = line_text.trimLeft();
                            const trimmed = left_trimmed.trimRight();
                            errors.push({
                                line,
                                col: line_text.length - left_trimmed.length,
                                length: trimmed.length,
                                description: `Expected ${param_count} arguments, got ${arg_tokens.length}.`,
                            });
                        } else if (arg_tokens.length === opcode.params.length) {
                            parse_args(opcode.params, arg_tokens, ins_args);
                        } else {
                            const stack_args: Arg[] = [];
                            parse_args(opcode.stack_params, arg_tokens, stack_args);

                            // TODO: proper error checking.
                            // TODO: UVars.
                            for (let i = 0; i < opcode.stack_params.length; i++) {
                                const param = opcode.stack_params[i];
                                const arg = stack_args[i];

                                switch (param.type) {
                                    case Type.U8:
                                    case Type.Register:
                                        instructions.push(new Instruction(Opcode.arg_pushb, [arg]));
                                        break;
                                    case Type.U16:
                                        instructions.push(new Instruction(Opcode.arg_pushw, [arg]));
                                        break;
                                    case Type.U32:
                                    case Type.I32:
                                    case Type.F32:
                                        instructions.push(new Instruction(Opcode.arg_pushl, [arg]));
                                        break;
                                    case Type.String:
                                        instructions.push(new Instruction(Opcode.arg_pushs, [arg]));
                                        break;
                                    default:
                                        throw new Error(
                                            `Type ${Type[param.type]} not implemented yet.`
                                        );
                                }
                            }
                        }
                    }

                    instructions.push(new Instruction(opcode, ins_args));
                }
            }
        }

        line++;
    }

    return {
        instructions,
        labels,
        errors,
    };
}

type ArgToken = {
    col: number;
    arg: string;
};

function tokenize_args(arg_str: string, col: number, args: ArgToken[]): boolean {
    if (arg_str.length === 0) {
        return true;
    }

    let match: RegExpMatchArray | null;

    if (args.length === 0) {
        match = arg_str.match(/^(?<arg_ws>\s+)(?<arg>"([^"\\]|\\.)*"|[^\s,]+)\s*/);
    } else {
        match = arg_str.match(/^(?<arg_ws>,\s*)(?<arg>"([^"\\]|\\.)*"|[^\s,]+)\s*/);
    }

    if (!match || !match.groups) {
        return false;
    } else {
        const { arg_ws, arg } = match.groups;
        args.push({
            col: col + arg_ws.length,
            arg,
        });

        return tokenize_args(arg_str.slice(match[0].length), col + match[0].length, args);
    }
}

// TODO: proper error checking.
// TODO: UVars.
function parse_args(params: Param[], arg_tokens: ArgToken[], args: Arg[]): void {
    for (let i = 0; i < params.length; i++) {
        const param = params[i];
        const arg_str = arg_tokens[i].arg;

        switch (param.type) {
            case Type.U8:
                args.push({
                    value: parseInt(arg_str, 10),
                    size: 1,
                });
                break;
            case Type.U16:
                args.push({
                    value: parseInt(arg_str, 10),
                    size: 2,
                });
                break;
            case Type.U32:
            case Type.I32:
            case Type.F32:
                args.push({
                    value: parseInt(arg_str, 10),
                    size: 4,
                });
                break;
            case Type.Register:
                args.push({
                    value: parseInt(arg_str.slice(1), 10),
                    size: 1,
                });
                break;
            case Type.String:
                {
                    const value: string = JSON.parse(arg_str);
                    args.push({
                        value,
                        size: 2 + 2 * value.length,
                    });
                }
                break;
            default:
                throw new Error(`Type ${Type[param.type]} not implemented yet.`);
        }
    }
}
