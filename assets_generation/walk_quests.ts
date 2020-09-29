import { readdirSync, readFileSync, statSync } from "fs";
import { parse_qst_to_quest, QuestData } from "../src/core/data_formats/parsing/quest";
import { BufferCursor } from "../src/core/data_formats/block/cursor/BufferCursor";
import { Endianness } from "../src/core/data_formats/block/Endianness";
import { LogManager } from "../src/core/logging";
import { Severity } from "../src/core/Severity";

const logger = LogManager.get("assets_generation/walk_quests");

/**
 * Applies process to all QST files in a directory. Uses the 106 QST files
 * provided with Tethealla version 0.143 by default.
 */
export function walk_quests(
    config: { path: string; suppress_parser_log?: boolean; exclude?: readonly string[] },
    process: (quest: QuestData) => void,
): void {
    const loggers = (config.suppress_parser_log !== false
        ? [
              "core/data_formats/asm/data_flow_analysis/register_value",
              "core/data_formats/parsing/quest",
              "core/data_formats/parsing/quest/bin",
              "core/data_formats/parsing/quest/object_code",
              "core/data_formats/parsing/quest/qst",
          ]
        : []
    ).map(logger_name => {
        const logger = LogManager.get(logger_name);
        const old = logger.severity;
        logger.severity = Severity.Error;
        return [logger, old] as const;
    });

    try {
        walk_qst_files(config, (p, _, contents) => {
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
        });
    } finally {
        for (const [logger, severity] of loggers) {
            logger.severity = severity;
        }
    }
}

/**
 * Applies f to all 106 QST files provided with Tethealla version 0.143.
 * f is called with the path to the file, the file name and the content of the file.
 */
export function walk_qst_files(
    config: { path?: string; exclude?: readonly string[] },
    f: (path: string, file_name: string, contents: Buffer) => void,
): void {
    const path = config.path ?? "assets_generation/resources/tethealla_v0.143_quests";

    // BUG: Battle quests are not always parsed in the same way.
    // Could be a bug in Jest or Node as the quest parsing code has no randomness or dependency on mutable state.
    // TODO: Some quests can not yet be parsed correctly.
    let exclude: readonly string[];

    if (config.exclude) {
        exclude = config.exclude;
    } else if (config.path == undefined) {
        exclude = [
            "/battle", // Battle mode quests.
            "/ep2/event/ma4-a.qst", // .qst seems corrupt, doesn't work in qedit either.
        ];
    } else {
        exclude = [];
    }

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
    if (exclude.some(e => path.includes(e))) {
        return;
    }

    const stat = statSync(path);

    if (stat.isFile()) {
        if (path.endsWith(".qst")) {
            f(path, name, readFileSync(path));
        }
    } else if (stat.isDirectory()) {
        for (const file of readdirSync(path)) {
            walk_qst_files_internal(f, `${path}/${file}`, file, exclude);
        }
    }
}
