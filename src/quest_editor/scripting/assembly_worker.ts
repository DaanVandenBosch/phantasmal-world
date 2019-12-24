import {
    AssemblyChangeInput,
    AssemblySettingsChangeInput,
    AssemblyWorkerInput,
    DefinitionInput,
    DefinitionOutput,
    InputMessageType,
    NewObjectCodeOutput,
    OutputMessageType,
    SignatureHelpInput,
    SignatureHelpOutput,
} from "./assembly_worker_messages";
import { assemble, AssemblySettings } from "./assembly";
import { AsmToken, Segment, SegmentType } from "./instructions";
import { Kind, OP_BB_MAP_DESIGNATE, Opcode, OPCODES_BY_MNEMONIC } from "./opcodes";
import { AssemblyLexer, IdentToken, TokenType } from "./AssemblyLexer";

const ctx: Worker = self as any;

let lines: string[] = [];
let object_code: Segment[] = [];
const line_no_to_instructions: {
    segment_index: number;
    instruction_indices: number[];
}[] = [];
const label_to_segment_cache: Map<number, Segment | null> = new Map();

const messages: AssemblyWorkerInput[] = [];
let timeout: any;

const assembly_settings: AssemblySettings = {
    manual_stack: false,
};

ctx.onmessage = (e: MessageEvent) => {
    messages.push(e.data);

    if (!timeout) {
        process_messages();

        timeout = setTimeout(() => {
            timeout = undefined;
            process_messages();
        }, 100);
    }
};

function process_messages(): void {
    if (messages.length === 0) return;

    for (const message of messages.splice(0, messages.length)) {
        switch (message.type) {
            case InputMessageType.NewAssembly:
                lines = message.assembly;
                assemble_and_send();
                break;
            case InputMessageType.AssemblyChange:
                assembly_change(message);
                break;
            case InputMessageType.SignatureHelp:
                signature_help(message);
                break;
            case InputMessageType.Definition:
                definition(message);
                break;
            case InputMessageType.SettingsChange:
                settings_change(message);
                break;
        }
    }
}

function assembly_change(message: AssemblyChangeInput): void {
    for (const change of message.changes) {
        const { start_line_no, end_line_no, start_col, end_col, new_text } = change;
        const lines_changed = end_line_no - start_line_no + 1;
        const new_lines = new_text.split("\n");

        if (lines_changed === 1) {
            replace_line_part(start_line_no, start_col, end_col, new_lines);
        } else if (new_lines.length === 1) {
            replace_lines_and_merge_line_parts(
                start_line_no,
                end_line_no,
                start_col,
                end_col,
                new_lines[0],
            );
        } else {
            // Keep the left part of the first changed line.
            replace_line_part_right(start_line_no, start_col, new_lines[0]);

            // Keep the right part of the last changed line.
            replace_line_part_left(end_line_no, end_col, new_lines[new_lines.length - 1]);

            // Replace all the lines in between.
            // It's important that we do this last.
            replace_lines(
                start_line_no + 1,
                end_line_no - 1,
                new_lines.slice(1, new_lines.length - 1),
            );
        }
    }

    assemble_and_send();
}

// Hacky way of providing parameter hints.
// We just tokenize the current line and look for the first identifier and check whether it's a valid opcode.
// TODO: make use of new meta information in IR.
function signature_help(message: SignatureHelpInput): void {
    let opcode: Opcode | undefined;
    let active_param = -1;

    if (message.line_no <= lines.length) {
        const line = lines[message.line_no - 1];
        const lexer = new AssemblyLexer();
        const tokens = lexer.tokenize_line(line);
        const ident = tokens.find(t => t.type === TokenType.Ident) as IdentToken | undefined;

        if (ident) {
            opcode = OPCODES_BY_MNEMONIC.get(ident.value);

            if (opcode) {
                for (const token of tokens) {
                    if (token.col + token.len > message.col) {
                        break;
                    } else if (token.type === TokenType.Ident && active_param === -1) {
                        active_param = 0;
                    } else if (token.type === TokenType.ArgSeparator) {
                        active_param++;
                    }
                }
            }
        }
    }

    const response: SignatureHelpOutput = {
        type: OutputMessageType.SignatureHelp,
        id: message.id,
        opcode,
        active_param,
    };
    ctx.postMessage(response);
}

function definition(message: DefinitionInput): void {
    const label = get_label_reference_at(message.line_no, message.col);
    let asm: AsmToken | undefined;

    if (label != undefined) {
        const segment = get_segment_by_label(label);

        if (segment) {
            const index = segment.labels.indexOf(label);

            if (index !== -1) {
                asm = segment.asm.labels[index];
            }
        }
    }

    const response: DefinitionOutput = {
        type: OutputMessageType.Definition,
        id: message.id,
        ...asm,
    };
    ctx.postMessage(response);
}

/**
 * Apply changes to settings.
 */
function settings_change(message: AssemblySettingsChangeInput): void {
    if (message.settings.manual_stack != undefined) {
        assembly_settings.manual_stack = message.settings.manual_stack;
    }
}

function assemble_and_send(): void {
    const assembler_result = assemble(lines, assembly_settings.manual_stack);

    object_code = assembler_result.object_code;
    label_to_segment_cache.clear();
    line_no_to_instructions.splice(0, Infinity);

    const map_designations = new Map<number, number>();

    for (let i = 0; i < object_code.length; i++) {
        const segment = object_code[i];

        if (segment.type === SegmentType.Instructions) {
            // Set map designations.
            if (segment.labels.includes(0)) {
                for (const inst of segment.instructions) {
                    if (inst.opcode.code === OP_BB_MAP_DESIGNATE.code) {
                        map_designations.set(inst.args[0].value, inst.args[2].value);
                    }
                }
            }

            // Index instructions by text position.
            for (let j = 0; j < segment.instructions.length; j++) {
                const ins = segment.instructions[j];

                if (ins.asm) {
                    if (ins.asm.mnemonic) {
                        add_index(ins.asm.mnemonic.line_no, i, j);
                    }

                    for (const arg_asm of ins.asm.args) {
                        add_index(arg_asm.line_no, i, j);
                    }
                }
            }
        }
    }

    const response: NewObjectCodeOutput = {
        type: OutputMessageType.NewObjectCode,
        map_designations,
        ...assembler_result,
    };
    ctx.postMessage(response);
}

function replace_line_part(
    line_no: number,
    start_col: number,
    end_col: number,
    new_line_parts: string[],
): void {
    const line = lines[line_no - 1];
    // We keep the parts of the line that weren't affected by the edit.
    const line_start = line.slice(0, start_col - 1);
    const line_end = line.slice(end_col - 1);

    if (new_line_parts.length === 1) {
        lines.splice(line_no - 1, 1, line_start + new_line_parts[0] + line_end);
    } else {
        lines.splice(
            line_no - 1,
            1,
            line_start + new_line_parts[0],
            ...new_line_parts.slice(1, new_line_parts.length - 1),
            new_line_parts[new_line_parts.length - 1] + line_end,
        );
    }
}

function replace_line_part_left(line_no: number, end_col: number, new_line_part: string): void {
    lines.splice(line_no - 1, 1, new_line_part + lines[line_no - 1].slice(end_col - 1));
}

function replace_line_part_right(line_no: number, start_col: number, new_line_part: string): void {
    lines.splice(line_no - 1, 1, lines[line_no - 1].slice(0, start_col - 1) + new_line_part);
}

function replace_lines(start_line_no: number, end_line_no: number, new_lines: string[]): void {
    lines.splice(start_line_no - 1, end_line_no - start_line_no + 1, ...new_lines);
}

function replace_lines_and_merge_line_parts(
    start_line_no: number,
    end_line_no: number,
    start_col: number,
    end_col: number,
    new_line_part: string,
): void {
    const start_line = lines[start_line_no - 1];
    const end_line = lines[end_line_no - 1];
    // We keep the parts of the lines that weren't affected by the edit.
    const start_line_start = start_line.slice(0, start_col - 1);
    const end_line_end = end_line.slice(end_col - 1);

    lines.splice(
        start_line_no - 1,
        end_line_no - start_line_no + 1,
        start_line_start + new_line_part + end_line_end,
    );
}

// TODO: make the code work with stack-based instructions
function get_label_reference_at(line_no: number, col: number): number | undefined {
    const handle = line_no_to_instructions[line_no];
    if (!handle) return undefined;

    const segment = object_code[handle.segment_index];
    if (!segment || segment.type !== SegmentType.Instructions) return undefined;

    for (const index of handle.instruction_indices) {
        const ins = segment.instructions[index];

        if (ins && ins.asm) {
            const params = ins.opcode.params;

            for (let i = 0; i < ins.asm.args.length; i++) {
                const param = i < params.length ? params[i] : params[params.length - 1];
                const arg_asm = ins.asm.args[i];

                if (
                    (param.type.kind === Kind.ILabel ||
                        param.type.kind === Kind.DLabel ||
                        param.type.kind === Kind.SLabel ||
                        param.type.kind === Kind.ILabelVar) &&
                    position_inside(line_no, col, arg_asm)
                ) {
                    return ins.args[i].value;
                }
            }

            for (let i = 0; i < ins.asm.stack_args.length; i++) {
                const param = i < params.length ? params[i] : params[params.length - 1];
                const arg_asm = ins.asm.stack_args[i];

                if (
                    (param.type.kind === Kind.ILabel ||
                        param.type.kind === Kind.DLabel ||
                        param.type.kind === Kind.SLabel ||
                        param.type.kind === Kind.ILabelVar) &&
                    position_inside(line_no, col, arg_asm)
                ) {
                    return arg_asm.value;
                }
            }
        }
    }

    return undefined;
}

function get_segment_by_label(label: number): Segment | undefined {
    let segment = label_to_segment_cache.get(label);

    // Strict comparison because null has special meaning.
    if (segment === undefined) {
        segment = null;

        for (const seg of object_code) {
            if (seg.labels.includes(label)) {
                segment = seg;
                break;
            }
        }

        label_to_segment_cache.set(label, segment);
    }

    return segment || undefined;
}

function add_index(line_no: number, segment_index: number, instruction_index: number): void {
    let handle = line_no_to_instructions[line_no];

    if (!handle) {
        handle = { segment_index, instruction_indices: [] };
        line_no_to_instructions[line_no] = handle;
    }

    handle.instruction_indices.push(instruction_index);
}

function position_inside(line_no: number, col: number, asm?: AsmToken): boolean {
    if (asm) {
        return line_no === asm.line_no && col >= asm.col && col <= asm.col + asm.len;
    } else {
        return false;
    }
}
