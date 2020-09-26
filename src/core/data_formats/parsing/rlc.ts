import { Endianness } from "../block/Endianness";
import { Cursor } from "../block/cursor/Cursor";
import { parse_prc } from "./prc";
import { LogManager } from "../../logging";
import { Result, ResultBuilder } from "../../Result";
import { Severity } from "../../Severity";

const logger = LogManager.get("core/data_formats/parsing/rlc");
const MARKER = "RelChunkVer0.20";

/**
 * Container of prc files.
 *
 * @returns the contained files, decrypted and decompressed.
 */
export function parse_rlc(cursor: Cursor): Result<Cursor[]> {
    const rb = new ResultBuilder<Cursor[]>(logger);
    const marker = cursor.string_ascii(16, true, true);

    if (marker !== MARKER) {
        rb.add_problem(
            Severity.Warning,
            "This file doesn't seem to be an RLC file.",
            `First 16 bytes where "${marker}" instead of expected "${MARKER}".`,
        );
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
        file.seek_start(0);

        const prc_result = parse_prc(file);
        rb.add_result(prc_result);

        if (!prc_result.success) {
            return rb.failure();
        }

        files.push(prc_result.value);

        cursor.seek_start(prev_pos);
    }

    return rb.success(files);
}
