import { Cursor } from "../cursor/Cursor";

export type Rel = {
    data_offset: number;
    index: RelIndexEntry[];
};

export type RelIndexEntry = {
    offset: number;
    size: number;
};

export function parse_rel(cursor: Cursor, parse_index: boolean): Rel {
    cursor.seek_end(32);

    const index_offset = cursor.u32();
    const index_size = cursor.u32();
    cursor.seek(8); // Typically 1, 0, 0,...
    const data_offset = cursor.u32();
    // Typically followed by 12 nul bytes.

    cursor.seek_start(index_offset);
    const index = parse_index ? parse_indices(cursor, index_size) : [];

    return { data_offset, index };
}

function parse_indices(cursor: Cursor, index_size: number): RelIndexEntry[] {
    const compact_offsets = cursor.u16_array(index_size);
    const index: RelIndexEntry[] = [];
    let expanded_offset = 0;

    for (const compact_offset of compact_offsets) {
        expanded_offset = expanded_offset + 4 * compact_offset;

        // Size is not always present.
        cursor.seek_start(expanded_offset - 4);
        const size = cursor.u32();
        const offset = cursor.u32();
        index.push({ offset, size });
    }

    return index;
}
