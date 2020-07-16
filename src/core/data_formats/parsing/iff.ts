import { Cursor } from "../block/cursor/Cursor";
import { Result, result_builder } from "../../Result";
import { LogManager } from "../../Logger";
import { Severity } from "../../Severity";

const logger = LogManager.get("core/data_formats/parsing/iff");

export type IffChunk = {
    /**
     * 32-bit unsigned integer.
     */
    readonly type: number;
    readonly data: Cursor;
};

export type IffChunkHeader = {
    /**
     * 32-bit unsigned integer.
     */
    readonly type: number;
    readonly size: number;
};

/**
 * PSO uses a little endian variant of the IFF format.
 * IFF files contain chunks preceded by an 8-byte header.
 * The header consists of 4 ASCII characters for the "Type ID" and a 32-bit integer specifying the chunk size.
 */
export function parse_iff(cursor: Cursor, silent = false): Result<IffChunk[]> {
    return parse(cursor, silent, [], (cursor, type, size) => {
        return { type, data: cursor.take(size) };
    });
}

/**
 * Parses just the chunk headers.
 */
export function parse_iff_headers(cursor: Cursor, silent = false): Result<IffChunkHeader[]> {
    return parse(cursor, silent, [], (_, type, size) => {
        return { type, size };
    });
}

function parse<T>(
    cursor: Cursor,
    silent: boolean,
    chunks: T[],
    get_chunk: (cursor: Cursor, type: number, size: number) => T,
): Result<T[]> {
    const result = result_builder<T[]>(logger);
    let corrupted = false;

    while (cursor.bytes_left >= 8) {
        const type = cursor.u32();
        const size_pos = cursor.position;
        const size = cursor.u32();

        if (size > cursor.bytes_left) {
            corrupted = true;

            if (!silent) {
                result.add_problem(
                    chunks.length === 0 ? Severity.Error : Severity.Warning,
                    "Invalid IFF format.",
                    `Size ${size} was too large (only ${cursor.bytes_left} bytes left) at position ${size_pos}.`,
                );
            }

            break;
        }

        chunks.push(get_chunk(cursor, type, size));
    }

    if (corrupted && chunks.length === 0) {
        return result.failure();
    } else {
        return result.success(chunks);
    }
}
