import * as fs from "fs";
import { InstructionSegment, SegmentType } from "../../src/quest_editor/scripting/instructions";
import { assemble } from "../../src/quest_editor/scripting/assembly";

/**
 * Applies f to all QST files in a directory.
 * f is called with the path to the file, the file name and the content of the file.
 * Uses the 106 QST files provided with Tethealla version 0.143 by default.
 */
export function walk_qst_files(
    f: (path: string, file_name: string, contents: Buffer) => void,
    dir = "test/resources/tethealla_v0.143_quests",
): void {
    for (const [path, file] of get_qst_files(dir)) {
        f(path, file, fs.readFileSync(path));
    }
}

export function get_qst_files(dir: string): [string, string][] {
    let files: [string, string][] = [];

    for (const file of fs.readdirSync(dir)) {
        const path = `${dir}/${file}`;
        const stats = fs.statSync(path);

        if (stats.isDirectory()) {
            files = files.concat(get_qst_files(path));
        } else if (path.endsWith(".qst")) {
            // BUG: Battle quests are not always parsed in the same way.
            // Could be a bug in Jest or Node as the quest parsing code has no randomness or dependency on mutable state.
            // TODO: Some quests can not yet be parsed correctly.
            const exceptions = [
                "/battle/", // Battle mode quests
                "/princ/", // Goverment quests
                "fragmentofmemoryen.qst",
                "lost havoc vulcan.qst",
                "ep2/event/ma4-a.qst",
                "gallon.qst",
                "ep1/04.qst",
                "goodluck.qst",
            ];

            if (exceptions.every(e => path.indexOf(e) === -1)) {
                files.push([path, file]);
            }
        }
    }

    return files;
}

export function to_instructions(assembly: string, manual_stack?: boolean): InstructionSegment[] {
    const { object_code, warnings, errors } = assemble(assembly.split("\n"), manual_stack);

    expect(warnings).toEqual([]);
    expect(errors).toEqual([]);

    return object_code.filter(
        segment => segment.type === SegmentType.Instructions,
    ) as InstructionSegment[];
}
