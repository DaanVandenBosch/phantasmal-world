import { Cursor } from "../block/cursor/Cursor";
import { LogManager } from "../../Logger";
import { Result, result_builder } from "../../Result";
import { Severity } from "../../Severity";

const logger = LogManager.get("core/data_formats/parsing/afs");

const AFS = 0x00534641;

/**
 * AFS is a trivial archive format used by SEGA for e.g. player character textures.
 *
 * @param cursor - The AFS archive
 * @returns the contained files
 */
export function parse_afs(cursor: Cursor): Result<ArrayBuffer[]> {
    const result = result_builder<ArrayBuffer[]>(logger);

    if (cursor.bytes_left < 8) {
        return result
            .add_problem(
                Severity.Error,
                "AFS archive is corrupted.",
                "Too small to be an AFS archive.",
            )
            .failure();
    }

    const magic = cursor.u32();

    if (magic !== AFS) {
        return result
            .add_problem(Severity.Error, "AFS archive is corrupted.", "Magic bytes not present.")
            .failure();
    }

    const file_count = cursor.u16();

    // Skip two unused bytes (are these just part of the file count field?).
    cursor.seek(2);

    const files: ArrayBuffer[] = [];

    for (let i = 1; i <= file_count; i++) {
        if (cursor.bytes_left < 8) {
            result.add_problem(
                Severity.Warning,
                `AFS file entry ${i} is invalid.`,
                `Couldn't read file entry ${i}, only ${cursor.bytes_left} bytes left.`,
            );

            break;
        }

        const offset = cursor.u32();
        const size = cursor.u32();

        if (offset > cursor.size) {
            result.add_problem(
                Severity.Warning,
                `AFS file entry ${i} is invalid.`,
                `Invalid file offset ${offset} for entry ${i}.`,
            );
        } else if (offset + size > cursor.size) {
            result.add_problem(
                Severity.Warning,
                `AFS file entry ${i} is invalid.`,
                `File size ${size} (offset: ${offset}) of entry ${i} too large.`,
            );
        } else {
            const start_pos = cursor.position;
            cursor.seek_start(offset);
            files.push(cursor.array_buffer(size));
            cursor.seek_start(start_pos);
        }
    }

    return result.success(files);
}
