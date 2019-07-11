import { Cursor } from "../cursor/Cursor";

export type IffChunk = {
    /**
     * 32-bit unsigned integer.
     */
    type: number;
    data: Cursor;
};

/**
 * PSO uses a little endian variant of the IFF format.
 * IFF files contain chunks preceded by an 8-byte header.
 * The header consists of 4 ASCII characters for the "Type ID" and a 32-bit integer specifying the chunk size.
 */
export function parse_iff(cursor: Cursor): IffChunk[] {
    const chunks: IffChunk[] = [];

    while (cursor.bytes_left) {
        const type = cursor.u32();
        const size = cursor.u32();

        if (size > cursor.bytes_left) {
            break;
        }

        chunks.push({
            type,
            data: cursor.take(size),
        });
    }

    return chunks;
}
