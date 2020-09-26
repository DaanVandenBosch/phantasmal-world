import { reinterpret_f32_as_i32 } from "../../core/primitive_conversion";
import {
    AssemblyLexer,
    CodeSectionToken,
    DataSectionToken,
    IdentToken,
    IntToken,
    LabelToken,
    RegisterToken,
    StringSectionToken,
    StringToken,
    Token,
    TokenType,
} from "./AssemblyLexer";
import {
    Arg,
    DataSegment,
    InstructionSegment,
    new_instruction,
    Segment,
    SegmentType,
    StringSegment,
} from "../../core/data_formats/asm/instructions";
import {
    Kind,
    OP_ARG_PUSHB,
    OP_ARG_PUSHL,
    OP_ARG_PUSHR,
    OP_ARG_PUSHS,
    OP_ARG_PUSHW,
    Opcode,
    OPCODES_BY_MNEMONIC,
    Param,
    StackInteraction,
} from "../../core/data_formats/asm/opcodes";
import { LogManager } from "../../core/logging";

const logger = LogManager.get("quest_editor/scripting/assembly");

export type AssemblyWarning = {
    line_no: number;
    col: number;
    length: number;
    message: string;
};

export type AssemblyError = AssemblyWarning;

export type AssemblySettings = {
    manual_stack: boolean;
};

export function assemble(
    assembly: string[],
    manual_stack = false,
): {
    object_code: Segment[];
    warnings: AssemblyWarning[];
    errors: AssemblyError[];
} {
    logger.trace("assemble start");

    const result = new Assembler(assembly, manual_stack).assemble();

    logger.trace(
        `assemble end with ${result.warnings.length} warnings and ${result.errors.length} errors.`,
    );

    return result;
}

class Assembler {
    private readonly lexer = new AssemblyLexer();
    private line_no!: number;
    private tokens!: Token[];
    private object_code!: Segment[];
    /**
     * The current segment.
     */
    private segment?: Segment;
    private warnings!: AssemblyWarning[];
    private errors!: AssemblyError[];
    /**
     * Encountered labels.
     */
    private labels!: Set<number>;
    private section: SegmentType = SegmentType.Instructions;
    private first_section_marker = true;
    private prev_line_had_label = false;

    constructor(private readonly assembly: string[], private readonly manual_stack: boolean) {}

    assemble(): {
        object_code: Segment[];
        warnings: AssemblyWarning[];
        errors: AssemblyError[];
    } {
        // Reset all state.
        this.line_no = 1;
        this.object_code = [];
        this.warnings = [];
        this.errors = [];
        this.labels = new Set();
        // Need to cast SegmentType.Instructions because of TypeScript bug.
        this.section = SegmentType.Instructions as SegmentType;
        this.first_section_marker = true;
        this.prev_line_had_label = false;

        // Tokenize and assemble line by line.
        for (const line of this.assembly) {
            this.tokens = this.lexer.tokenize_line(line);

            if (this.tokens.length > 0) {
                const token = this.tokens.shift()!;
                let has_label = false;

                switch (token.type) {
                    case TokenType.Label:
                        this.parse_label(token);
                        has_label = true;
                        break;
                    case TokenType.CodeSection:
                    case TokenType.DataSection:
                    case TokenType.StringSection:
                        this.parse_section(token);
                        break;
                    case TokenType.Int:
                        if (this.section === SegmentType.Data) {
                            this.parse_bytes(token);
                        } else {
                            this.add_error({
                                col: token.col,
                                length: token.len,
                                message: "Unexpected token.",
                            });
                        }
                        break;
                    case TokenType.String:
                        if (this.section === SegmentType.String) {
                            this.parse_string(token);
                        } else {
                            this.add_error({
                                col: token.col,
                                length: token.len,
                                message: "Unexpected token.",
                            });
                        }
                        break;
                    case TokenType.Ident:
                        if (this.section === SegmentType.Instructions) {
                            this.parse_instruction(token);
                        } else {
                            this.add_error({
                                col: token.col,
                                length: token.len,
                                message: "Unexpected token.",
                            });
                        }
                        break;
                    case TokenType.InvalidSection:
                        this.add_error({
                            col: token.col,
                            length: token.len,
                            message: "Invalid section type.",
                        });
                        break;
                    case TokenType.InvalidIdent:
                        this.add_error({
                            col: token.col,
                            length: token.len,
                            message: "Invalid identifier.",
                        });
                        break;
                    default:
                        this.add_error({
                            col: token.col,
                            length: token.len,
                            message: "Unexpected token.",
                        });
                        break;
                }

                this.prev_line_had_label = has_label;
            }

            this.line_no++;
        }

        return {
            object_code: this.object_code,
            warnings: this.warnings,
            errors: this.errors,
        };
    }

    private add_instruction(
        opcode: Opcode,
        args: Arg[],
        stack_args: Arg[],
        token: Token | undefined,
        arg_tokens: Token[],
        stack_arg_tokens: Token[],
    ): void {
        if (!this.segment) {
            // Unreachable code, technically valid.
            const instruction_segment: InstructionSegment = {
                labels: [],
                type: SegmentType.Instructions,
                instructions: [],
                asm: {
                    labels: [],
                },
            };

            this.segment = instruction_segment;
            this.object_code.push(instruction_segment);
        } else if (this.segment.type === SegmentType.Instructions) {
            this.segment.instructions.push(
                new_instruction(opcode, args, {
                    mnemonic: token && {
                        line_no: this.line_no,
                        col: token.col,
                        len: token.len,
                    },
                    args: arg_tokens.map(arg_t => ({
                        line_no: this.line_no,
                        col: arg_t.col,
                        len: arg_t.len,
                    })),
                    stack_args: stack_arg_tokens.map((arg_t, i) => ({
                        line_no: this.line_no,
                        col: arg_t.col,
                        len: arg_t.len,
                        value: stack_args[i].value,
                    })),
                }),
            );
        } else {
            logger.error(`Line ${this.line_no}: Expected instructions segment.`);
        }
    }

    private add_bytes(bytes: number[]): void {
        if (!this.segment) {
            // Unaddressable data, technically valid.
            const data_segment: DataSegment = {
                labels: [],
                type: SegmentType.Data,
                data: new Uint8Array(bytes).buffer,
                asm: {
                    labels: [],
                },
            };

            this.segment = data_segment;
            this.object_code.push(data_segment);
        } else if (this.segment.type === SegmentType.Data) {
            const buf = new ArrayBuffer(this.segment.data.byteLength + bytes.length);
            const arr = new Uint8Array(buf);

            arr.set(new Uint8Array(this.segment.data));
            arr.set(new Uint8Array(bytes), this.segment.data.byteLength);

            this.segment.data = buf;
        } else {
            logger.error(`Line ${this.line_no}: Expected data segment.`);
        }
    }

    private add_string(str: string): void {
        if (!this.segment) {
            // Unaddressable data, technically valid.
            const string_segment: StringSegment = {
                labels: [],
                type: SegmentType.String,
                value: str,
                asm: {
                    labels: [],
                },
            };

            this.segment = string_segment;
            this.object_code.push(string_segment);
        } else if (this.segment.type === SegmentType.String) {
            this.segment.value += str;
        } else {
            logger.error(`Line ${this.line_no}: Expected string segment.`);
        }
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

    private add_warning({
        col,
        length,
        message,
    }: {
        col: number;
        length: number;
        message: string;
    }): void {
        this.warnings.push({
            line_no: this.line_no,
            col,
            length,
            message,
        });
    }

    private parse_label({ col, len, value: label }: LabelToken): void {
        if (this.labels.has(label)) {
            this.add_error({
                col,
                length: len,
                message: "Duplicate label.",
            });
        }

        this.labels.add(label);

        const next_token = this.tokens.shift();

        const asm = { line_no: this.line_no, col, len };

        if (this.prev_line_had_label) {
            const segment = this.object_code[this.object_code.length - 1];
            segment.labels.push(label);
            segment.asm.labels.push(asm);
        }

        switch (this.section) {
            case SegmentType.Instructions:
                if (!this.prev_line_had_label) {
                    this.segment = {
                        type: SegmentType.Instructions,
                        labels: [label],
                        instructions: [],
                        asm: { labels: [asm] },
                    };
                    this.object_code.push(this.segment);
                }

                if (next_token) {
                    if (next_token.type === TokenType.Ident) {
                        this.parse_instruction(next_token);
                    } else {
                        this.add_error({
                            col: next_token.col,
                            length: next_token.len,
                            message: "Expected opcode mnemonic.",
                        });
                    }
                }

                break;

            case SegmentType.Data:
                if (!this.prev_line_had_label) {
                    this.segment = {
                        type: SegmentType.Data,
                        labels: [label],
                        data: new ArrayBuffer(0),
                        asm: {
                            labels: [asm],
                        },
                    };
                    this.object_code.push(this.segment);
                }

                if (next_token) {
                    if (next_token.type === TokenType.Int) {
                        this.parse_bytes(next_token);
                    } else {
                        this.add_error({
                            col: next_token.col,
                            length: next_token.len,
                            message: "Expected bytes.",
                        });
                    }
                }

                break;

            case SegmentType.String:
                if (!this.prev_line_had_label) {
                    this.segment = {
                        type: SegmentType.String,
                        labels: [label],
                        value: "",
                        asm: {
                            labels: [asm],
                        },
                    };
                    this.object_code.push(this.segment);
                }

                if (next_token) {
                    if (next_token.type === TokenType.String) {
                        this.parse_string(next_token);
                    } else {
                        this.add_error({
                            col: next_token.col,
                            length: next_token.len,
                            message: "Expected a string.",
                        });
                    }
                }

                break;
        }
    }

    private parse_section({
        type,
        col,
        len,
    }: CodeSectionToken | DataSectionToken | StringSectionToken): void {
        let section!: SegmentType;

        switch (type) {
            case TokenType.CodeSection:
                section = SegmentType.Instructions;
                break;
            case TokenType.DataSection:
                section = SegmentType.Data;
                break;
            case TokenType.StringSection:
                section = SegmentType.String;
                break;
        }

        if (this.section === section && !this.first_section_marker) {
            this.add_warning({
                col,
                length: len,
                message: "Unnecessary section marker.",
            });
        }

        this.section = section;
        this.first_section_marker = false;

        const next_token = this.tokens.shift();

        if (next_token) {
            this.add_error({
                col: next_token.col,
                length: next_token.len,
                message: "Unexpected token.",
            });
        }
    }

    private parse_instruction(ident_token: IdentToken): void {
        const { col, len, value } = ident_token;
        const opcode = OPCODES_BY_MNEMONIC.get(value);

        if (!opcode) {
            this.add_error({
                col,
                length: len,
                message: "Unknown instruction.",
            });
        } else {
            const varargs =
                opcode.params.findIndex(
                    p => p.type.kind === Kind.ILabelVar || p.type.kind === Kind.RegRefVar,
                ) !== -1;

            const param_count =
                this.manual_stack && opcode.stack === StackInteraction.Pop
                    ? 0
                    : opcode.params.length;

            let arg_count = 0;

            for (const token of this.tokens) {
                if (token.type !== TokenType.ArgSeparator) {
                    arg_count++;
                }
            }

            const last_token = this.tokens[this.tokens.length - 1];
            const error_length = last_token ? last_token.col + last_token.len - col : 0;
            // Inline arguments.
            const ins_arg_and_tokens: [Arg, Token][] = [];
            // Stack arguments.
            const stack_arg_and_tokens: [Arg, Token][] = [];

            if (!varargs && arg_count !== param_count) {
                this.add_error({
                    col,
                    length: error_length,
                    message: `Expected ${param_count} argument${
                        param_count === 1 ? "" : "s"
                    }, got ${arg_count}.`,
                });

                return;
            } else if (varargs && arg_count < param_count) {
                this.add_error({
                    col,
                    length: error_length,
                    message: `Expected at least ${param_count} argument${
                        param_count === 1 ? "" : "s"
                    }, got ${arg_count}.`,
                });

                return;
            } else if (opcode.stack !== StackInteraction.Pop) {
                // Inline arguments.
                if (!this.parse_args(opcode.params, ins_arg_and_tokens, false)) {
                    return;
                }
            } else {
                if (!this.parse_args(opcode.params, stack_arg_and_tokens, true)) {
                    return;
                }

                for (let i = 0; i < opcode.params.length; i++) {
                    const param = opcode.params[i];
                    const arg_and_token = stack_arg_and_tokens[i];

                    if (arg_and_token == undefined) {
                        continue;
                    }

                    const [arg, arg_token] = arg_and_token;

                    if (arg_token.type === TokenType.Register) {
                        if (param.type.kind === Kind.RegTupRef) {
                            this.add_instruction(
                                OP_ARG_PUSHB,
                                [arg],
                                [],
                                undefined,
                                [arg_token],
                                [],
                            );
                        } else {
                            this.add_instruction(
                                OP_ARG_PUSHR,
                                [arg],
                                [],
                                undefined,
                                [arg_token],
                                [],
                            );
                        }
                    } else {
                        switch (param.type.kind) {
                            case Kind.Byte:
                            case Kind.RegRef:
                            case Kind.RegTupRef:
                                this.add_instruction(
                                    OP_ARG_PUSHB,
                                    [arg],
                                    [],
                                    undefined,
                                    [arg_token],
                                    [],
                                );
                                break;
                            case Kind.Word:
                            case Kind.Label:
                            case Kind.ILabel:
                            case Kind.DLabel:
                            case Kind.SLabel:
                                this.add_instruction(
                                    OP_ARG_PUSHW,
                                    [arg],
                                    [],
                                    undefined,
                                    [arg_token],
                                    [],
                                );
                                break;
                            case Kind.DWord:
                                this.add_instruction(
                                    OP_ARG_PUSHL,
                                    [arg],
                                    [],
                                    undefined,
                                    [arg_token],
                                    [],
                                );
                                break;
                            case Kind.Float:
                                this.add_instruction(
                                    OP_ARG_PUSHL,
                                    [{ value: reinterpret_f32_as_i32(arg.value) }],
                                    [],
                                    undefined,
                                    [arg_token],
                                    [],
                                );
                                break;
                            case Kind.String:
                                this.add_instruction(
                                    OP_ARG_PUSHS,
                                    [arg],
                                    [],
                                    undefined,
                                    [arg_token],
                                    [],
                                );
                                break;
                            default:
                                logger.error(
                                    `Line ${this.line_no}: Type ${
                                        Kind[param.type.kind]
                                    } not implemented.`,
                                );
                                break;
                        }
                    }
                }
            }

            const args: Arg[] = [];
            const arg_tokens: Token[] = [];

            const stack_args: Arg[] = [];
            const stack_arg_tokens: Token[] = [];

            for (const [arg, token] of ins_arg_and_tokens) {
                args.push(arg);
                arg_tokens.push(token);
            }

            for (const [arg, token] of stack_arg_and_tokens) {
                stack_args.push(arg);
                stack_arg_tokens.push(token);
            }

            this.add_instruction(
                opcode,
                args,
                stack_args,
                ident_token,
                arg_tokens,
                stack_arg_tokens,
            );
        }
    }

    /**
     * @returns true if arguments can be translated to object code, possibly after truncation. False otherwise.
     */
    private parse_args(
        params: readonly Param[],
        arg_and_tokens: [Arg, Token][],
        stack: boolean,
    ): boolean {
        let semi_valid = true;
        let should_be_arg = true;
        let param_i = 0;

        for (let i = 0; i < this.tokens.length; i++) {
            const token = this.tokens[i];
            const param = params[param_i];

            if (token.type === TokenType.ArgSeparator) {
                if (should_be_arg) {
                    this.add_error({
                        col: token.col,
                        length: token.len,
                        message: "Expected an argument.",
                    });
                } else if (
                    param.type.kind !== Kind.ILabelVar &&
                    param.type.kind !== Kind.RegRefVar
                ) {
                    param_i++;
                }

                should_be_arg = true;
            } else {
                if (!should_be_arg) {
                    const prev_token = this.tokens[i - 1];
                    const col = prev_token.col + prev_token.len;

                    this.add_error({
                        col,
                        length: token.col - col,
                        message: "Expected a comma.",
                    });
                }

                should_be_arg = false;

                let match: boolean;

                switch (token.type) {
                    case TokenType.Int:
                        switch (param.type.kind) {
                            case Kind.Byte:
                                match = true;
                                this.parse_int(1, token, arg_and_tokens);
                                break;
                            case Kind.Word:
                            case Kind.Label:
                            case Kind.ILabel:
                            case Kind.DLabel:
                            case Kind.SLabel:
                            case Kind.ILabelVar:
                                match = true;
                                this.parse_int(2, token, arg_and_tokens);
                                break;
                            case Kind.DWord:
                                match = true;
                                this.parse_int(4, token, arg_and_tokens);
                                break;
                            case Kind.Float:
                                match = true;
                                arg_and_tokens.push([{ value: token.value }, token]);
                                break;
                            default:
                                match = false;
                                break;
                        }
                        break;
                    case TokenType.Float:
                        match = param.type.kind === Kind.Float;

                        if (match) {
                            arg_and_tokens.push([{ value: token.value }, token]);
                        }

                        break;
                    case TokenType.Register:
                        match =
                            stack ||
                            param.type.kind === Kind.RegRef ||
                            param.type.kind === Kind.RegRefVar ||
                            param.type.kind === Kind.RegTupRef;

                        this.parse_register(token, arg_and_tokens);
                        break;
                    case TokenType.String:
                        match = param.type.kind === Kind.String;

                        if (match) {
                            arg_and_tokens.push([{ value: token.value }, token]);
                        }

                        break;
                    default:
                        match = false;
                        break;
                }

                if (!match) {
                    semi_valid = false;

                    let type_str: string | undefined;

                    switch (param.type.kind) {
                        case Kind.Byte:
                            type_str = "an 8-bit integer";
                            break;
                        case Kind.Word:
                            type_str = "a 16-bit integer";
                            break;
                        case Kind.DWord:
                            type_str = "a 32-bit integer";
                            break;
                        case Kind.Float:
                            type_str = "a float";
                            break;
                        case Kind.Label:
                            type_str = "a label";
                            break;
                        case Kind.ILabel:
                        case Kind.ILabelVar:
                            type_str = "an instruction label";
                            break;
                        case Kind.DLabel:
                            type_str = "a data label";
                            break;
                        case Kind.SLabel:
                            type_str = "a string label";
                            break;
                        case Kind.String:
                            type_str = "a string";
                            break;
                        case Kind.RegRef:
                        case Kind.RegRefVar:
                        case Kind.RegTupRef:
                            type_str = "a register reference";
                            break;
                    }

                    if (type_str) {
                        this.add_error({
                            col: token.col,
                            length: token.len,
                            message: `Expected ${type_str}.`,
                        });
                    } else {
                        this.add_error({
                            col: token.col,
                            length: token.len,
                            message: `Unexpected token.`,
                        });
                    }
                }
            }
        }

        this.tokens = [];
        return semi_valid;
    }

    private parse_int(size: number, token: IntToken, arg_and_tokens: [Arg, Token][]): void {
        const { value, col, len } = token;
        const bit_size = 8 * size;
        const min_value = -Math.pow(2, bit_size - 1);
        const max_value = Math.pow(2, bit_size) - 1;

        if (value < min_value) {
            this.add_error({
                col,
                length: len,
                message: `${bit_size}-Bit integer can't be less than ${min_value}.`,
            });
        } else if (value > max_value) {
            this.add_error({
                col,
                length: len,
                message: `${bit_size}-Bit integer can't be greater than ${max_value}.`,
            });
        } else {
            arg_and_tokens.push([{ value }, token]);
        }
    }

    private parse_register(token: RegisterToken, arg_and_tokens: [Arg, Token][]): void {
        const { col, len, value } = token;

        if (value > 255) {
            this.add_error({
                col,
                length: len,
                message: `Invalid register reference, expected r0-r255.`,
            });
        } else {
            arg_and_tokens.push([{ value }, token]);
        }
    }

    private parse_bytes(first_token: IntToken): void {
        const bytes = [];
        let token: Token = first_token;
        let i = 0;

        while (token.type === TokenType.Int) {
            if (token.value < 0) {
                this.add_error({
                    col: token.col,
                    length: token.len,
                    message: "Unsigned 8-bit integer can't be less than 0.",
                });
            } else if (token.value > 255) {
                this.add_error({
                    col: token.col,
                    length: token.len,
                    message: "Unsigned 8-bit integer can't be greater than 255.",
                });
            }

            bytes.push(token.value);

            if (i < this.tokens.length) {
                token = this.tokens[i++];
            } else {
                break;
            }
        }

        if (i < this.tokens.length) {
            this.add_error({
                col: token.col,
                length: token.len,
                message: "Expected an unsigned 8-bit integer.",
            });
        }

        this.add_bytes(bytes);
    }

    private parse_string(token: StringToken): void {
        const next_token = this.tokens.shift();

        if (next_token) {
            this.add_error({
                col: next_token.col,
                length: next_token.len,
                message: "Unexpected token.",
            });
        }

        this.add_string(token.value.replace(/\n/g, "<cr>"));
    }
}
