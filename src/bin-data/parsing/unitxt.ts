import { ArrayBufferCursor } from "../ArrayBufferCursor";
import { decompress } from "../compression/prs";

export type Unitxt = string[][];

export function parseUnitxt(buf: ArrayBufferCursor, compressed: boolean = true): Unitxt {
    if (compressed) {
        buf = decompress(buf);
    }

    const categoryCount = buf.u32();
    const entryCounts = buf.u32Array(categoryCount);
    const categoryEntryOffsets: Array<Array<number>> = [];

    for (const entryCount of entryCounts) {
        categoryEntryOffsets.push(buf.u32Array(entryCount));
    }

    const categories: Unitxt = [];

    for (const categoryEntryOffset of categoryEntryOffsets) {
        const entries: string[] = [];
        categories.push(entries);

        for (const entryOffset of categoryEntryOffset) {
            buf.seekStart(entryOffset);
            const str = buf.stringUtf16(1024, true, true);
            entries.push(str);
        }
    }

    return categories;
}
