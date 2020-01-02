import { Endianness } from "../../Endianness";
import { Cursor } from "../../cursor/Cursor";
import { LogManager } from "../../../Logger";
import { ArrayBufferCursor } from "../../cursor/ArrayBufferCursor";
import { assert } from "../../../util";

const logger = LogManager.get("core/data_formats/parsing/quest/bin");

export type BinFile = {
    readonly quest_id: number;
    readonly language: number;
    readonly quest_name: string;
    readonly short_description: string;
    readonly long_description: string;
    readonly object_code: ArrayBuffer;
    readonly label_offsets: readonly number[];
    readonly shop_items: readonly number[];
};

export function parse_bin(cursor: Cursor): { bin: BinFile; dc_gc_format: boolean } {
    const object_code_offset = cursor.u32();
    const label_offset_table_offset = cursor.u32(); // Relative offsets
    const size = cursor.u32();
    cursor.seek(4); // Always seems to be 0xFFFFFFFF for BB.

    const dc_gc_format = object_code_offset !== 4652;

    let quest_id: number;
    let language: number;

    if (dc_gc_format) {
        language = cursor.u16();
        quest_id = cursor.u16();
    } else {
        quest_id = cursor.u32();
        language = cursor.u32();
    }

    const quest_name = dc_gc_format
        ? cursor.string_ascii(32, true, true)
        : cursor.string_utf16(64, true, true);
    const short_description = dc_gc_format
        ? cursor.string_ascii(128, true, true)
        : cursor.string_utf16(256, true, true);
    const long_description = dc_gc_format
        ? cursor.string_ascii(288, true, true)
        : cursor.string_utf16(576, true, true);

    if (size !== cursor.size) {
        logger.warn(`Value ${size} in bin size field does not match actual size ${cursor.size}.`);
    }

    cursor.seek(4); // Skip padding.

    const shop_items = cursor.u32_array(932);

    const label_offset_count = Math.floor((cursor.size - label_offset_table_offset) / 4);
    cursor.seek_start(label_offset_table_offset);

    const label_offsets = cursor.i32_array(label_offset_count);

    const object_code = cursor
        .seek_start(object_code_offset)
        .array_buffer(label_offset_table_offset - object_code_offset);

    return {
        bin: {
            quest_id,
            language,
            quest_name,
            short_description,
            long_description,
            object_code,
            label_offsets,
            shop_items,
        },
        dc_gc_format,
    };
}

export function write_bin(bin: BinFile): ArrayBuffer {
    const object_code_offset = 4652;
    const file_size =
        object_code_offset + bin.object_code.byteLength + 4 * bin.label_offsets.length;
    const buffer = new ArrayBuffer(file_size);
    const cursor = new ArrayBufferCursor(buffer, Endianness.Little);

    cursor.write_u32(object_code_offset);
    cursor.write_u32(object_code_offset + bin.object_code.byteLength); // Label table offset.
    cursor.write_u32(file_size);
    cursor.write_u32(0xffffffff);
    cursor.write_u32(bin.quest_id);
    cursor.write_u32(bin.language);
    cursor.write_string_utf16(bin.quest_name, 64);
    cursor.write_string_utf16(bin.short_description, 256);
    cursor.write_string_utf16(bin.long_description, 576);
    cursor.write_u32(0);

    if (bin.shop_items.length > 932) {
        throw new Error(`shop_items can't be larger than 932, was ${bin.shop_items.length}.`);
    }

    cursor.write_u32_array(bin.shop_items);

    for (let i = bin.shop_items.length; i < 932; i++) {
        cursor.write_u32(0);
    }

    while (cursor.position < object_code_offset) {
        cursor.write_u8(0);
    }

    cursor.write_cursor(new ArrayBufferCursor(bin.object_code, Endianness.Little));

    cursor.write_i32_array(bin.label_offsets);

    assert(
        cursor.position === file_size,
        `Expected to write ${file_size} bytes, but wrote ${cursor.position}.`,
    );

    return buffer;
}
