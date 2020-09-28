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
import { LogManager } from "../../src/core/logging";
import { Severity } from "../../src/core/Severity";
import { Disposer } from "../../src/core/observable/Disposer";
import { try_finally } from "../../src/core/util";
import * as walk_quests from "../../assets_generation/walk_quests";

export function pw_test(
    { max_log_severity = Severity.Info }: { max_log_severity?: Severity },
    f: (disposer: Disposer) => void,
): () => void {
    return () => {
        const disposer = new Disposer();

        const log: string[] = [];
        const orig_severity = LogManager.default_severity;
        const orig_handler = LogManager.default_handler;
        LogManager.default_severity = max_log_severity + 1;
        LogManager.default_handler = entry => log.push(entry.message);

        return try_finally(
            () => f(disposer),
            () => {
                disposer.dispose();

                // Reset LogManager after disposing the disposer, because disposing it could trigger
                // logging.
                LogManager.default_severity = orig_severity;
                LogManager.default_handler = orig_handler;

                expect(log).toEqual([]);
            },
        );
    };
}

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
    dir: string,
): void {
    walk_quests.walk_qst_files(f, dir);
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
