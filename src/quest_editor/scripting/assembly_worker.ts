import {
    AssemblyChangeInput,
    AssemblyWorkerInput,
    InputMessageType,
    NewObjectCodeOutput,
    OutputMessageType,
    SignatureHelpInput,
    SignatureHelpOutput,
    AssemblySettingsChangeInput,
} from "./assembly_worker_messages";
import { assemble, AssemblySettings } from "./assembly";
import Logger from "js-logger";
import { SegmentType } from "./instructions";
import { Opcode, OPCODES_BY_MNEMONIC } from "./opcodes";
import { AssemblyLexer, IdentToken, TokenType } from "./AssemblyLexer";

Logger.useDefaults({
    defaultLevel: (Logger as any)[process.env["LOG_LEVEL"] || "OFF"],
});

const ctx: Worker = self as any;

let lines: string[] = [];

const messages: AssemblyWorkerInput[] = [];
let timeout: any;

const assembly_settings: AssemblySettings = {
    manual_stack: false
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
function signature_help(message: SignatureHelpInput): void {
    let opcode: Opcode | undefined;
    let active_param = -1;

    if (message.line_no < lines.length) {
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

/**
 * Apply changes to settings.
 */
function settings_change(message: AssemblySettingsChangeInput): void {
    if (message.settings.hasOwnProperty("manual_stack")) {
        assembly_settings.manual_stack = Boolean(message.settings.manual_stack);
    }
}

function assemble_and_send(): void {
    const assembler_result = assemble(lines, assembly_settings.manual_stack);
    const map_designations = new Map<number, number>();

    for (const segment of assembler_result.object_code) {
        if (segment.labels.includes(0)) {
            if (segment.type === SegmentType.Instructions) {
                for (const inst of segment.instructions) {
                    if (inst.opcode === Opcode.BB_MAP_DESIGNATE) {
                        map_designations.set(inst.args[0].value, inst.args[2].value);
                    }
                }
            }

            break;
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
