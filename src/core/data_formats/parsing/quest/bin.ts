import { Endianness } from "../../block/Endianness";
import { Cursor } from "../../block/cursor/Cursor";
import { LogManager } from "../../../Logger";
import { ArrayBufferCursor } from "../../block/cursor/ArrayBufferCursor";
import { assert } from "../../../util";
import { BinFormat } from "./BinFormat";

const logger = LogManager.get("core/data_formats/parsing/quest/bin");

const DC_GC_OBJECT_CODE_OFFSET = 468;
const PC_OBJECT_CODE_OFFSET = 920;
const BB_OBJECT_CODE_OFFSET = 4652;

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

export function parse_bin(cursor: Cursor): { bin: BinFile; format: BinFormat } {
    const object_code_offset = cursor.u32();
    const label_offset_table_offset = cursor.u32(); // Relative offsets
    const size = cursor.u32();
    cursor.seek(4); // Always seems to be 0xFFFFFFFF.

    let format: number;

    switch (object_code_offset) {
        case DC_GC_OBJECT_CODE_OFFSET:
            format = BinFormat.DC_GC;
            break;
        case BB_OBJECT_CODE_OFFSET:
            format = BinFormat.BB;
            break;
        default:
            format = BinFormat.PC;
            break;
    }

    let quest_id: number;
    let language: number;
    let quest_name: string;
    let short_description: string;
    let long_description: string;

    if (format === BinFormat.DC_GC) {
        cursor.seek(1);
        language = cursor.u8();
        quest_id = cursor.u16();
        quest_name = cursor.string_ascii(32, true, true);
        short_description = cursor.string_ascii(128, true, true);
        long_description = cursor.string_ascii(288, true, true);
    } else {
        quest_id = cursor.u32();
        language = cursor.u32();
        quest_name = cursor.string_utf16(64, true, true);
        short_description = cursor.string_utf16(256, true, true);
        long_description = cursor.string_utf16(576, true, true);
    }

    if (size !== cursor.size) {
        logger.warn(`Value ${size} in bin size field does not match actual size ${cursor.size}.`);
    }

    let shop_items: number[];

    if (format === BinFormat.BB) {
        cursor.seek(4); // Skip padding.
        shop_items = cursor.u32_array(932);
    } else {
        shop_items = [];
    }

    const label_offset_count = Math.floor((cursor.size - label_offset_table_offset) / 4);
    const label_offsets = cursor
        .seek_start(label_offset_table_offset)
        .i32_array(label_offset_count);

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
        format,
    };
}

export function write_bin(bin: BinFile, format: BinFormat): ArrayBuffer {
    assert(
        bin.quest_name.length <= 32,
        () => `quest_name can't be longer than 32 characters, was ${bin.quest_name.length}`,
    );
    assert(
        bin.short_description.length <= 127,
        () =>
            `short_description can't be longer than 127 characters, was ${bin.short_description.length}`,
    );
    assert(
        bin.long_description.length <= 287,
        () =>
            `long_description can't be longer than 287 characters, was ${bin.long_description.length}`,
    );
    assert(
        bin.shop_items.length === 0 || format === BinFormat.BB,
        "shop_items is only supported in BlueBurst quests.",
    );
    assert(
        bin.shop_items.length <= 932,
        () => `shop_items can't be larger than 932, was ${bin.shop_items.length}.`,
    );

    let object_code_offset: number;

    switch (format) {
        case BinFormat.DC_GC:
            object_code_offset = DC_GC_OBJECT_CODE_OFFSET;
            break;
        case BinFormat.PC:
            object_code_offset = PC_OBJECT_CODE_OFFSET;
            break;
        case BinFormat.BB:
            object_code_offset = BB_OBJECT_CODE_OFFSET;
            break;
    }

    const file_size =
        object_code_offset + bin.object_code.byteLength + 4 * bin.label_offsets.length;
    const buffer = new ArrayBuffer(file_size);
    const cursor = new ArrayBufferCursor(buffer, Endianness.Little);

    cursor.write_u32(object_code_offset);
    cursor.write_u32(object_code_offset + bin.object_code.byteLength); // Label table offset.
    cursor.write_u32(file_size);
    cursor.write_u32(0xffffffff);

    if (format === BinFormat.DC_GC) {
        cursor.write_u8(0);
        cursor.write_u8(bin.language);
        cursor.write_u16(bin.quest_id);
        cursor.write_string_ascii(bin.quest_name, 32);
        cursor.write_string_ascii(bin.short_description, 128);
        cursor.write_string_ascii(bin.long_description, 288);
    } else {
        cursor.write_u32(bin.quest_id);
        cursor.write_u32(bin.language);
        cursor.write_string_utf16(bin.quest_name, 64);
        cursor.write_string_utf16(bin.short_description, 256);
        cursor.write_string_utf16(bin.long_description, 576);
    }

    if (format === BinFormat.BB) {
        cursor.write_u32(0);
        cursor.write_u32_array(bin.shop_items);

        for (let i = bin.shop_items.length; i < 932; i++) {
            cursor.write_u32(0);
        }
    }

    assert(
        cursor.position === object_code_offset,
        () =>
            `Expected to write ${object_code_offset} bytes before object code, but wrote ${cursor.position}.`,
    );

    cursor.write_cursor(new ArrayBufferCursor(bin.object_code, Endianness.Little));

    cursor.write_i32_array(bin.label_offsets);

    assert(
        cursor.position === file_size,
        `Expected to write ${file_size} bytes, but wrote ${cursor.position}.`,
    );

    return buffer;
}
