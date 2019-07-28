import {
    Arg,
    Instruction,
    InstructionSegment,
    Opcode,
    OPCODES_BY_MNEMONIC,
    Param,
    Segment,
    SegmentType,
    Type,
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
    object_code: Segment[];
    errors: AssemblyError[];
} {
    return new Assembler(assembly, manual_stack).assemble();
}

type ArgToken = {
    col: number;
    arg: string;
};

class Assembler {
    private line_no!: number;
    private object_code!: Segment[];
    private errors!: AssemblyError[];
    // Encountered labels.
    private labels!: Set<number>;

    constructor(private assembly: string[], private manual_stack: boolean) {}

    assemble(): {
        object_code: Segment[];
        errors: AssemblyError[];
    } {
        this.line_no = 1;
        this.object_code = [];
        this.errors = [];
        this.labels = new Set();

        for (const line of this.assembly) {
            const match = line.match(
                /^(?<lbl_ws>\s*)(?<lbl>[^\s]+?:)?(?<op_ws>\s*)(?<op>[a-z][a-z0-9_=<>!]*)?(?<args>.*)$/
            );

            if (
                !match ||
                !match.groups ||
                (match.groups.lbl == undefined && match.groups.op == undefined)
            ) {
                const left_trimmed = line.trimLeft();
                const trimmed = left_trimmed.trimRight();

                if (trimmed.length) {
                    this.add_error({
                        col: 1 + line.length - left_trimmed.length,
                        length: trimmed.length,
                        message: "Expected label or instruction.",
                    });
                }
            } else {
                const { lbl_ws, lbl, op_ws, op, args } = match.groups;

                if (lbl != undefined) {
                    this.parse_label(lbl, lbl_ws);
                }

                if (op != undefined) {
                    this.parse_instruction(
                        1 + lbl_ws.length + (lbl ? lbl.length : 0) + op_ws.length,
                        op,
                        args
                    );
                }
            }

            this.line_no++;
        }

        return {
            object_code: this.object_code,
            errors: this.errors,
        };
    }

    private add_instruction(opcode: Opcode, args: Arg[]): void {
        const { instructions } = this.object_code[
            this.object_code.length - 1
        ] as InstructionSegment;

        instructions.push(new Instruction(opcode, args));
    }

    private add_error({
        col,
        length,
        message,
    }: {
        col: number;
        length: number;
        message: string;
    }): void {
        this.errors.push({
            line_no: this.line_no,
            col,
            length,
            message,
        });
    }

    private parse_label(lbl: string, lbl_ws: string): void {
        const label = parseInt(lbl.slice(0, -1), 10);

        if (!isFinite(label) || !/^\d+:$/.test(lbl)) {
            this.add_error({
                col: 1 + lbl_ws.length,
                length: lbl.length,
                message: "Invalid label name.",
            });
        } else {
            if (this.labels.has(label)) {
                this.add_error({
                    col: 1 + lbl_ws.length,
                    length: lbl.length - 1,
                    message: "Duplicate label.",
                });
            }

            this.object_code.push({
                type: SegmentType.Instructions,
                label,
                instructions: [],
            });
        }
    }

    private parse_instruction(col: number, op: string, args: string): void {
        const opcode = OPCODES_BY_MNEMONIC.get(op);

        if (!opcode) {
            this.add_error({
                col,
                length: op.length,
                message: "Unknown instruction.",
            });
        } else {
            const args_col = col + (op ? op.length : 0);

            const arg_tokens: ArgToken[] = [];
            const args_tokenization_ok = this.tokenize_args(args, args_col, arg_tokens);

            const ins_args: Arg[] = [];

            if (!args_tokenization_ok) {
                const left_trimmed = args.trimLeft();
                const trimmed = args.trimRight();

                this.add_error({
                    col: args_col + args.length - left_trimmed.length,
                    length: trimmed.length,
                    message: "Instruction arguments expected.",
                });
            } else {
                const varargs =
                    opcode.params.findIndex(
                        p => p.type === Type.U8Var || p.type === Type.ILabelVar
                    ) !== -1;

                const param_count =
                    opcode.params.length + (this.manual_stack ? 0 : opcode.stack_params.length);

                if (varargs ? arg_tokens.length < param_count : arg_tokens.length !== param_count) {
                    this.add_error({
                        col,
                        length: op.length + args.trimRight().length,
                        message: `Expected${varargs ? " at least" : ""} ${param_count} argument${
                            param_count === 1 ? "" : "s"
                        }, got ${arg_tokens.length}.`,
                    });
                } else if (varargs || arg_tokens.length === opcode.params.length) {
                    this.parse_args(opcode.params, arg_tokens, ins_args);
                } else {
                    const stack_args: Arg[] = [];
                    this.parse_args(opcode.stack_params, arg_tokens, stack_args);

                    for (let i = 0; i < opcode.stack_params.length; i++) {
                        const param = opcode.stack_params[i];
                        const arg = stack_args[i];
                        const col = arg_tokens[i].col;
                        const length = arg_tokens[i].arg.length;

                        if (arg == undefined) {
                            continue;
                        }

                        switch (param.type) {
                            case Type.U8:
                            case Type.Register:
                                this.add_instruction(Opcode.arg_pushb, [arg]);
                                break;
                            case Type.U16:
                            case Type.ILabel:
                            case Type.DLabel:
                                this.add_instruction(Opcode.arg_pushw, [arg]);
                                break;
                            case Type.U32:
                            case Type.I32:
                            case Type.F32:
                                this.add_instruction(Opcode.arg_pushl, [arg]);
                                break;
                            case Type.String:
                                this.add_instruction(Opcode.arg_pushs, [arg]);
                                break;
                            default:
                                this.add_error({
                                    col,
                                    length,
                                    message: `Type ${Type[param.type]} not implemented.`,
                                });
                        }
                    }
                }
            }

            this.add_instruction(opcode, ins_args);
        }
    }

    private tokenize_args(arg_str: string, col: number, args: ArgToken[]): boolean {
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

            return this.tokenize_args(arg_str.slice(match[0].length), col + match[0].length, args);
        }
    }

    private parse_args(params: Param[], arg_tokens: ArgToken[], args: Arg[]): void {
        for (let i = 0; i < params.length; i++) {
            const param = params[i];
            const arg_token = arg_tokens[i];
            const arg_str = arg_token.arg;
            const col = arg_token.col;
            const length = arg_str.length;

            switch (param.type) {
                case Type.U8:
                    this.parse_uint(arg_str, 1, args, col);
                    break;
                case Type.U16:
                case Type.ILabel:
                case Type.DLabel:
                    this.parse_uint(arg_str, 2, args, col);
                    break;
                case Type.U32:
                    this.parse_uint(arg_str, 4, args, col);
                    break;
                case Type.I32:
                    this.parse_sint(arg_str, 4, args, col);
                    break;
                case Type.F32:
                    this.parse_float(arg_str, args, col);
                    break;
                case Type.Register:
                    this.parse_register(arg_str, args, col);
                    break;
                case Type.String:
                    this.parse_string(arg_str, args, col);
                    break;
                case Type.U8Var:
                    this.parse_uint_varargs(arg_tokens, i, 1, args);
                    return;
                case Type.ILabelVar:
                    this.parse_uint_varargs(arg_tokens, i, 2, args);
                    return;
                default:
                    this.add_error({
                        col,
                        length,
                        message: `Type ${Type[param.type]} not implemented.`,
                    });
                    break;
            }
        }
    }

    private parse_uint(arg_str: string, size: number, args: Arg[], col: number): void {
        const bit_size = 8 * size;
        const value = parseInt(arg_str, 10);
        const max_value = Math.pow(2, bit_size) - 1;

        if (!/^\d+$/.test(arg_str)) {
            this.add_error({
                col,
                length: arg_str.length,
                message: `Expected unsigned integer.`,
            });
        } else if (value > max_value) {
            this.add_error({
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

    private parse_sint(arg_str: string, size: number, args: Arg[], col: number): void {
        const bit_size = 8 * size;
        const value = parseInt(arg_str, 10);
        const min_value = -Math.pow(2, bit_size - 1);
        const max_value = Math.pow(2, bit_size - 1) - 1;

        if (!/^-?\d+$/.test(arg_str)) {
            this.add_error({
                col,
                length: arg_str.length,
                message: `Expected signed integer.`,
            });
        } else if (value < min_value) {
            this.add_error({
                col,
                length: arg_str.length,
                message: `${bit_size}-Bit signed integer can't be less than ${min_value}.`,
            });
        } else if (value > max_value) {
            this.add_error({
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

    private parse_float(arg_str: string, args: Arg[], col: number): void {
        const value = parseFloat(arg_str);

        if (!Number.isFinite(value)) {
            this.add_error({
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

    private parse_register(arg_str: string, args: Arg[], col: number): void {
        const value = parseInt(arg_str.slice(1), 10);

        if (!/^r\d+$/.test(arg_str)) {
            this.add_error({
                col,
                length: arg_str.length,
                message: `Expected register reference.`,
            });
        } else if (value > 255) {
            this.add_error({
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

    private parse_string(arg_str: string, args: Arg[], col: number): void {
        if (!/^"([^"\\]|\\.)*"$/.test(arg_str)) {
            this.add_error({
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

    private parse_uint_varargs(
        arg_tokens: ArgToken[],
        index: number,
        size: number,
        args: Arg[]
    ): void {
        for (; index < arg_tokens.length; index++) {
            const arg_token = arg_tokens[index];
            const col = arg_token.col;
            this.parse_uint(arg_token.arg, size, args, col);
        }
    }
}
