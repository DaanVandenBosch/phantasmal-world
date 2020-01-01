import { Cursor } from "../cursor/Cursor";
import { LogManager } from "../../Logger";

const logger = LogManager.get("core/data_formats/parsing/afs");

const AFS = 0x41465300;

type AfsFileEntry = {
    readonly offset: number;
    readonly size: number;
};

/**
 * AFS is a trivial archive format used by SEGA for e.g. player character textures.
 *
 * @param cursor - The AFS archive
 * @returns the contained files
 */
export function parse_afs(cursor: Cursor): ArrayBuffer[] {
    const magic = cursor.u32();

    if (magic !== AFS) {
        logger.error("Not an AFS archive.");
        return [];
    }

    const file_count = cursor.u16();

    // Skip two unused bytes.
    cursor.seek(2);

    const file_entries: AfsFileEntry[] = [];

    for (let i = 0; i < file_count; i++) {
        const offset = cursor.u32();
        const size = cursor.u32();

        file_entries.push({ offset, size });
    }

    const files: ArrayBuffer[] = [];

    for (const { offset, size } of file_entries) {
        cursor.seek_start(offset);
        files.push(cursor.array_buffer(size));
    }

    return files;
}
