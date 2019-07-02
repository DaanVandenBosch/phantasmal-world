import { BufferCursor } from "../BufferCursor";
import { decompress } from "../compression/prs";

export type Unitxt = string[][];

export function parse_unitxt(buf: BufferCursor, compressed: boolean = true): Unitxt {
    if (compressed) {
        buf = decompress(buf);
    }

    const category_count = buf.u32();
    const entry_counts = buf.u32_array(category_count);
    const category_entry_offsets: number[][] = [];

    for (const entry_count of entry_counts) {
        category_entry_offsets.push(buf.u32_array(entry_count));
    }

    const categories: Unitxt = [];

    for (const category_entry_offset of category_entry_offsets) {
        const entries: string[] = [];
        categories.push(entries);

        for (const entry_offset of category_entry_offset) {
            buf.seek_start(entry_offset);
            const str = buf.string_utf16(1024, true, true);
            entries.push(str);
        }
    }

    return categories;
}
