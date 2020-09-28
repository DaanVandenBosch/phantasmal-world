import { readdirSync, readFileSync, statSync } from "fs";
import { parse_qst_to_quest, QuestData } from "../src/core/data_formats/parsing/quest";
import { BufferCursor } from "../src/core/data_formats/block/cursor/BufferCursor";
import { Endianness } from "../src/core/data_formats/block/Endianness";
import { LogManager } from "../src/core/logging";
import { Severity } from "../src/core/Severity";

const logger = LogManager.get("assets_generation/walk_quests");

export function walk_quests(
    path: string,
    process: (quest: QuestData) => void,
    suppress_parser_log: boolean = true,
): void {
    const loggers = (suppress_parser_log
        ? [
              LogManager.get("core/data_formats/asm/data_flow_analysis/register_value"),
              LogManager.get("core/data_formats/parsing/quest"),
              LogManager.get("core/data_formats/parsing/quest/bin"),
              LogManager.get("core/data_formats/parsing/quest/object_code"),
              LogManager.get("core/data_formats/parsing/quest/qst"),
          ]
        : []
    ).map(logger => {
        const old = logger.severity;
        logger.severity = Severity.Error;
        return [logger, old] as const;
    });

    try {
        walk_qst_files(
            (p, _, contents) => {
                try {
                    const result = parse_qst_to_quest(
                        new BufferCursor(contents, Endianness.Little),
                        true,
                    );

                    if (result.success) {
                        process(result.value);
                    } else {
                        logger.error(`Couldn't process ${p}.`);
                    }
                } catch (e) {
                    logger.error(`Couldn't parse ${p}.`, e);
                }
            },
            path,
            [],
        );
    } finally {
        for (const [logger, severity] of loggers) {
            logger.severity = severity;
        }
    }
}

/**
 * Applies f to all QST files in a directory.
 * f is called with the path to the file, the file name and the content of the file.
 * Uses the 106 QST files provided with Tethealla version 0.143 by default.
 */
export function walk_qst_files(
    f: (path: string, file_name: string, contents: Buffer) => void,
    path = "assets_generation/resources/tethealla_v0.143_quests",
    // BUG: Battle quests are not always parsed in the same way.
    // Could be a bug in Jest or Node as the quest parsing code has no randomness or dependency on mutable state.
    // TODO: Some quests can not yet be parsed correctly.
    exclude: readonly string[] = [
        "/battle/", // Battle mode quests.
        "ep2/event/ma4-a.qst", // .qst seems corrupt, doesn't work in qedit either.
    ],
): void {
    const idx = path.lastIndexOf("/");
    walk_qst_files_internal(
        f,
        path,
        idx === -1 || idx >= path.length - 1 ? path : path.slice(idx + 1),
        exclude,
    );
}

function walk_qst_files_internal(
    f: (path: string, file_name: string, contents: Buffer) => void,
    path: string,
    name: string,
    exclude: readonly string[],
): void {
    const stat = statSync(path);

    if (stat.isFile()) {
        if (path.endsWith(".qst") && !exclude.some(e => path.includes(e))) {
            f(path, name, readFileSync(path));
        }
    } else if (stat.isDirectory()) {
        for (const file of readdirSync(path)) {
            walk_qst_files_internal(f, `${path}/${file}`, name, exclude);
        }
    }
}
