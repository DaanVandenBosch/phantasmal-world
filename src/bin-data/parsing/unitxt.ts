import { BufferCursor } from "../BufferCursor";
import { decompress } from "../compression/prs";

export type Unitxt = string[][];

export function parseUnitxt(buf: BufferCursor, compressed: boolean = true): Unitxt {
    if (compressed) {
        buf = decompress(buf);
    }

    const categoryCount = buf.u32();
    const entryCounts = buf.u32_array(categoryCount);
    const categoryEntryOffsets: Array<Array<number>> = [];

    for (const entryCount of entryCounts) {
        categoryEntryOffsets.push(buf.u32_array(entryCount));
    }

    const categories: Unitxt = [];

    for (const categoryEntryOffset of categoryEntryOffsets) {
        const entries: string[] = [];
        categories.push(entries);

        for (const entryOffset of categoryEntryOffset) {
            buf.seek_start(entryOffset);
            const str = buf.string_utf16(1024, true, true);
            entries.push(str);
        }
    }

    return categories;
}
