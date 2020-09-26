import * as fs from "fs";
import { InstructionSegment, SegmentType } from "../../src/core/data_formats/asm/instructions";
import { assemble } from "../../src/quest_editor/scripting/assembly";
import { parse_qst_to_quest } from "../../src/core/data_formats/parsing/quest";
import { BufferCursor } from "../../src/core/data_formats/block/cursor/BufferCursor";
import { Endianness } from "../../src/core/data_formats/block/Endianness";
import { Quest } from "../../src/core/data_formats/parsing/quest/Quest";
import { QuestModel } from "../../src/quest_editor/model/QuestModel";
import { AreaStore } from "../../src/quest_editor/stores/AreaStore";
import { convert_quest_to_model } from "../../src/quest_editor/stores/model_conversion";
import { unwrap } from "../../src/core/Result";

export async function timeout(millis: number): Promise<void> {
    return new Promise(resolve => {
        setTimeout(() => resolve(), millis);
    });
}

export function next_animation_frame(): Promise<void> {
    return new Promise(resolve => requestAnimationFrame(() => resolve()));
}

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

export function load_default_quest_model(area_store: AreaStore): QuestModel {
    return convert_quest_to_model(
        area_store,
        load_qst_as_quest("assets/quests/defaults/default_ep_1.qst"),
    );
}

export function load_qst_as_quest(path: string): Quest {
    return unwrap(parse_qst_to_quest(new BufferCursor(fs.readFileSync(path), Endianness.Little)))
        .quest;
}

export function to_instructions(assembly: string, manual_stack?: boolean): InstructionSegment[] {
    const { object_code, warnings, errors } = assemble(assembly.split("\n"), manual_stack);

    expect(warnings).toEqual([]);
    expect(errors).toEqual([]);

    return object_code.filter(
        segment => segment.type === SegmentType.Instructions,
    ) as InstructionSegment[];
}
