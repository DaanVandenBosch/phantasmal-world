import Logger from "js-logger";
import { Endianness } from "..";
import { Cursor } from "../cursor/Cursor";
import { parse_prc } from "./prc";

const logger = Logger.get("data_formats/parsing/rlc");
const MARKER = "RelChunkVer0.20";

/**
 * Container of prc files.
 *
 * @returns the contained files, decrypted and decompressed.
 */
export function parse_rlc(cursor: Cursor): Cursor[] {
    const marker = cursor.string_ascii(16, true, true);

    if (marker !== MARKER) {
        logger.warn(`First 16 bytes where "${marker}" instead of expected "${MARKER}".`);
    }

    const table_size = cursor.u32();
    cursor.seek(12);

    const files: Cursor[] = [];

    for (let i = 0; i < table_size; ++i) {
        const offset = cursor.u32();
        const size = cursor.u32();
        const prev_pos = cursor.position;

        cursor.seek_start(offset);

        const file = cursor.take(size);
        file.endianness = Endianness.Little;
        files.push(parse_prc(file));

        cursor.seek_start(prev_pos);
    }

    return files;
}
