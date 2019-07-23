import { editor } from "monaco-editor";
import { assemble, AssemblyError } from "./assembly";

interface ScriptWorkerInput {}

class NewModelInput implements ScriptWorkerInput {
    constructor(readonly value: string) {}
}

class ModelChangeInput implements ScriptWorkerInput {
    constructor(readonly changes: editor.IModelContentChange[]) {}
}

interface ScriptWorkerOutput {}

class NewErrorsOutput implements ScriptWorkerOutput {
    constructor(readonly errors: AssemblyError[]) {}
}

let lines: string[] = [];

function replace_line_part(
    line_no: number,
    start_col: number,
    end_col: number,
    new_line_parts: string[]
): void {
    const line = lines[line_no - 1];
    // We keep the parts of the line that weren't affected by the edit.
    const line_start = line.slice(0, start_col - 1);
    const line_end = line.slice(end_col);

    if (new_line_parts.length === 1) {
        lines.splice(line_no - 1, 1, line_start + new_line_parts[0] + line_end);
    } else {
        lines.splice(
            line_no - 1,
            1,
            line_start + new_line_parts[0],
            ...new_line_parts.slice(1, new_line_parts.length - 2),
            new_line_parts[new_line_parts.length - 1] + line_end
        );
    }
}

function replace_line_part_left(line_no: number, end_col: number, new_line_part: string): void {
    lines.splice(line_no - 1, 1, new_line_part + lines[line_no - 1].slice(end_col));
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
    const end_line_end = end_line.slice(end_col);

    lines.splice(
        start_line_no - 1,
        end_line_no - start_line_no + 1,
        start_line_start + new_line_part + end_line_end
    );
}

window.onmessage = (e: MessageEvent) => {
    const message: ScriptWorkerInput = e.data;

    if (message instanceof NewModelInput) {
        lines = message.value.split("\n");
        window.postMessage(assemble(lines).errors, window.origin);
    } else if (message instanceof ModelChangeInput) {
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

                // Replace all the lines in between.
                replace_lines(
                    startLineNumber + 1,
                    endLineNumber - 1,
                    new_lines.slice(1, new_lines.length - 2)
                );

                // Keep the right part of the last changed line.
                replace_line_part_left(startLineNumber, endColumn, new_lines[new_lines.length - 1]);
            }
        }

        window.postMessage(assemble(lines).errors, window.origin);
    } else {
        throw new Error("Couldn't process message.");
    }
};
