import { prs_decompress } from "../compression/prs/decompress";
import { Cursor } from "../block/cursor/Cursor";
import { Result, success } from "../../Result";

export type Unitxt = string[][];

export function parse_unitxt(buf: Cursor, compressed: boolean = true): Result<Unitxt> {
    if (compressed) {
        const decompression_result = prs_decompress(buf);

        if (!decompression_result.success) {
            return decompression_result;
        }

        buf = decompression_result.value;
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
            const str = buf.string_utf16(Math.min(1024, buf.bytes_left), true, true);
            entries.push(str);
        }
    }

    return success(categories);
}
