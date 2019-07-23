import {
    Instruction,
    OPCODES_BY_MNEMONIC,
    Arg,
    Type,
    Opcode,
    Param,
} from "../data_formats/parsing/quest/bin";

export type AssemblyError = {
    line_no: number;
    col: number;
    length: number;
    message: string;
};

export function assemble(
    assembly: string[],
    manual_stack: boolean = false
): {
    instructions: Instruction[];
    labels: Map<number, number>;
    errors: AssemblyError[];
} {
    const errors: AssemblyError[] = [];
    const instructions: Instruction[] = [];
    const labels = new Map<number, number>();

    let line_no = 1;

    for (const line of assembly) {
        const match = line.match(
            /^(?<lbl_ws>\s*)(?<lbl>[^\s]+?:)?(?<op_ws>\s*)(?<op>[a-z][a-z0-9_=<>!]*)?(?<args>.*)$/
        );

        if (!match || !match.groups || (match.groups.lbl == null && match.groups.op == null)) {
            const left_trimmed = line.trimLeft();
            const trimmed = left_trimmed.trimRight();

            if (trimmed.length) {
                errors.push({
                    line_no,
                    col: 1 + line.length - left_trimmed.length,
                    length: trimmed.length,
                    message: "Expected label or instruction.",
                });
            }
        } else {
            const { lbl_ws, lbl, op_ws, op, args } = match.groups;

            if (lbl != null) {
                const label = parseInt(lbl.slice(0, -1), 10);

                if (!isFinite(label) || !/^\d+:$/.test(lbl)) {
                    errors.push({
                        line_no,
                        col: 1 + lbl_ws.length,
                        length: lbl.length,
                        message: "Invalid label name.",
                    });
                } else if (labels.has(label)) {
                    errors.push({
                        line_no,
                        col: 1 + lbl_ws.length,
                        length: lbl.length - 1,
                        message: "Duplicate label.",
                    });
                } else {
                    labels.set(label, instructions.length);
                }
            }

            if (op != null) {
                const opcode = OPCODES_BY_MNEMONIC.get(op);

                if (!opcode) {
                    errors.push({
                        line_no,
                        col: 1 + lbl_ws.length + (lbl ? lbl.length : 0) + op_ws.length,
                        length: op.length,
                        message: "Unknown instruction.",
                    });
                } else {
                    const args_col =
                        1 +
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

                        errors.push({
                            line_no,
                            col: args_col + args.length - left_trimmed.length,
                            length: trimmed.length,
                            message: "Instruction arguments expected.",
                        });
                    } else {
                        const varargs =
                            opcode.params.findIndex(
                                p => p.type === Type.U8Var || p.type === Type.U16Var
                            ) !== -1;

                        const param_count =
                            opcode.params.length + (manual_stack ? 0 : opcode.stack_params.length);

                        if (
                            varargs
                                ? arg_tokens.length < param_count
                                : arg_tokens.length !== param_count
                        ) {
                            const left_trimmed = line.trimLeft();
                            errors.push({
                                line_no,
                                col: 1 + line.length - left_trimmed.length,
                                length: left_trimmed.length,
                                message: `Expected${
                                    varargs ? " at least" : ""
                                } ${param_count} argument${param_count === 1 ? "" : "s"}, got ${
                                    arg_tokens.length
                                }.`,
                            });
                        } else if (varargs || arg_tokens.length === opcode.params.length) {
                            parse_args(opcode.params, arg_tokens, ins_args, line_no, errors);
                        } else {
                            const stack_args: Arg[] = [];
                            parse_args(
                                opcode.stack_params,
                                arg_tokens,
                                stack_args,
                                line_no,
                                errors
                            );

                            for (let i = 0; i < opcode.stack_params.length; i++) {
                                const param = opcode.stack_params[i];
                                const arg = stack_args[i];
                                const col = arg_tokens[i].col;
                                const length = arg_tokens[i].arg.length;

                                if (arg == null) {
                                    continue;
                                }

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
                                        errors.push({
                                            line_no,
                                            col,
                                            length,
                                            message: `Type ${Type[param.type]} not implemented.`,
                                        });
                                }
                            }
                        }
                    }

                    instructions.push(new Instruction(opcode, ins_args));
                }
            }
        }

        line_no++;
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
    if (arg_str.trim().length === 0) {
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

function parse_args(
    params: Param[],
    arg_tokens: ArgToken[],
    args: Arg[],
    line: number,
    errors: AssemblyError[]
): void {
    for (let i = 0; i < params.length; i++) {
        const param = params[i];
        const arg_token = arg_tokens[i];
        const arg_str = arg_token.arg;
        const col = arg_token.col;
        const length = arg_str.length;

        switch (param.type) {
            case Type.U8:
                parse_uint(arg_str, 1, args, line, col, errors);
                break;
            case Type.U16:
                parse_uint(arg_str, 2, args, line, col, errors);
                break;
            case Type.U32:
                parse_uint(arg_str, 4, args, line, col, errors);
                break;
            case Type.I32:
                parse_sint(arg_str, 4, args, line, col, errors);
                break;
            case Type.F32:
                parse_float(arg_str, args, line, col, errors);
                break;
            case Type.Register:
                parse_register(arg_str, args, line, col, errors);
                break;
            case Type.String:
                parse_string(arg_str, args, line, col, errors);
                break;
            case Type.U8Var:
                parse_uint_varargs(arg_tokens, i, 1, args, line, errors);
                return;
            case Type.U16Var:
                parse_uint_varargs(arg_tokens, i, 2, args, line, errors);
                return;
            default:
                errors.push({
                    line_no: line,
                    col,
                    length,
                    message: `Type ${Type[param.type]} not implemented.`,
                });
        }
    }
}

function parse_uint(
    arg_str: string,
    size: number,
    args: Arg[],
    line: number,
    col: number,
    errors: AssemblyError[]
): void {
    const bit_size = 8 * size;
    const value = parseInt(arg_str, 10);
    const max_value = Math.pow(2, bit_size) - 1;

    if (!/^\d+$/.test(arg_str)) {
        errors.push({
            line_no: line,
            col,
            length: arg_str.length,
            message: `Expected unsigned integer.`,
        });
    } else if (value > max_value) {
        errors.push({
            line_no: line,
            col,
            length: arg_str.length,
            message: `${bit_size}-Bit unsigned integer can't be greater than ${max_value}.`,
        });
    } else {
        args.push({
            value,
            size,
        });
    }
}

function parse_sint(
    arg_str: string,
    size: number,
    args: Arg[],
    line: number,
    col: number,
    errors: AssemblyError[]
): void {
    const bit_size = 8 * size;
    const value = parseInt(arg_str, 10);
    const min_value = -Math.pow(2, bit_size - 1);
    const max_value = Math.pow(2, bit_size - 1) - 1;

    if (!/^-?\d+$/.test(arg_str)) {
        errors.push({
            line_no: line,
            col,
            length: arg_str.length,
            message: `Expected signed integer.`,
        });
    } else if (value < min_value) {
        errors.push({
            line_no: line,
            col,
            length: arg_str.length,
            message: `${bit_size}-Bit signed integer can't be less than ${min_value}.`,
        });
    } else if (value > max_value) {
        errors.push({
            line_no: line,
            col,
            length: arg_str.length,
            message: `${bit_size}-Bit signed integer can't be greater than ${max_value}.`,
        });
    } else {
        args.push({
            value,
            size,
        });
    }
}

function parse_float(
    arg_str: string,
    args: Arg[],
    line: number,
    col: number,
    errors: AssemblyError[]
): void {
    const value = parseFloat(arg_str);

    if (!Number.isFinite(value)) {
        errors.push({
            line_no: line,
            col,
            length: arg_str.length,
            message: `Expected floating point number.`,
        });
    } else {
        args.push({
            value,
            size: 4,
        });
    }
}

function parse_register(
    arg_str: string,
    args: Arg[],
    line: number,
    col: number,
    errors: AssemblyError[]
): void {
    const value = parseInt(arg_str.slice(1), 10);

    if (!/^r\d+$/.test(arg_str)) {
        errors.push({
            line_no: line,
            col,
            length: arg_str.length,
            message: `Expected register reference.`,
        });
    } else if (value > 255) {
        errors.push({
            line_no: line,
            col,
            length: arg_str.length,
            message: `Invalid register reference, expected r0-r255.`,
        });
    } else {
        args.push({
            value,
            size: 1,
        });
    }
}

function parse_string(
    arg_str: string,
    args: Arg[],
    line: number,
    col: number,
    errors: AssemblyError[]
): void {
    if (!/^"([^"\\]|\\.)*"$/.test(arg_str)) {
        errors.push({
            line_no: line,
            col,
            length: arg_str.length,
            message: `Expected string.`,
        });
    } else {
        const value = JSON.parse(arg_str);
        args.push({
            value,
            size: 2 + 2 * value.length,
        });
    }
}

function parse_uint_varargs(
    arg_tokens: ArgToken[],
    index: number,
    size: number,
    args: Arg[],
    line: number,
    errors: AssemblyError[]
): void {
    for (; index < arg_tokens.length; index++) {
        const arg_token = arg_tokens[index];
        const col = arg_token.col;
        parse_uint(arg_token.arg, size, args, line, col, errors);
    }
}
