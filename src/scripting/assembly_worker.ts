import { NewObjectCodeOutput, ScriptWorkerInput } from "./assembler_messages";
import { assemble } from "./assembly";

const ctx: Worker = self as any;

let lines: string[] = [];
const messages: ScriptWorkerInput[] = [];
let timeout: any;

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
        if (message.type === "new_assembly_input") {
            lines = message.assembly;
        } else if (message.type === "assembly_change_input") {
            for (const change of message.changes) {
                const { startLineNumber, endLineNumber, startColumn, endColumn } = change.range;
                const lines_changed = endLineNumber - startLineNumber + 1;
                const new_lines = change.text.split("\n");

                if (lines_changed === 1) {
                    replace_line_part(startLineNumber, startColumn, endColumn, new_lines);
                } else if (new_lines.length === 1) {
                    replace_lines_and_merge_line_parts(
                        startLineNumber,
                        endLineNumber,
                        startColumn,
                        endColumn,
                        new_lines[0]
                    );
                } else {
                    // Keep the left part of the first changed line.
                    replace_line_part_right(startLineNumber, startColumn, new_lines[0]);

                    // Keep the right part of the last changed line.
                    replace_line_part_left(
                        endLineNumber,
                        endColumn,
                        new_lines[new_lines.length - 1]
                    );

                    // Replace all the lines in between.
                    // It's important that we do this last.
                    replace_lines(
                        startLineNumber + 1,
                        endLineNumber - 1,
                        new_lines.slice(1, new_lines.length - 1)
                    );
                }
            }
        }
    }

    const response: NewObjectCodeOutput = {
        type: "new_object_code_output",
        ...assemble(lines),
    };
    ctx.postMessage(response);
}

function replace_line_part(
    line_no: number,
    start_col: number,
    end_col: number,
    new_line_parts: string[]
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
            new_line_parts[new_line_parts.length - 1] + line_end
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
    new_line_part: string
): void {
    const start_line = lines[start_line_no - 1];
    const end_line = lines[end_line_no - 1];
    // We keep the parts of the lines that weren't affected by the edit.
    const start_line_start = start_line.slice(0, start_col - 1);
    const end_line_end = end_line.slice(end_col - 1);

    lines.splice(
        start_line_no - 1,
        end_line_no - start_line_no + 1,
        start_line_start + new_line_part + end_line_end
    );
}
