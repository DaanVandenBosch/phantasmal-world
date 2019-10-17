import Logger from "js-logger";
import { Endianness } from "../../Endianness";
import { ArrayBufferCursor } from "../../cursor/ArrayBufferCursor";
import { Cursor } from "../../cursor/Cursor";
import { ResizableBufferCursor } from "../../cursor/ResizableBufferCursor";
import { WritableCursor } from "../../cursor/WritableCursor";
import { ResizableBuffer } from "../../ResizableBuffer";
import { basename } from "../../../util";

const logger = Logger.get("core/data_formats/parsing/quest/qst");

export type QstContainedFile = {
    id?: number;
    name: string;
    name_2?: string; // Unsure what this is
    data: ArrayBuffer;
};

export type ParseQstResult = {
    version: string;
    files: QstContainedFile[];
};

/**
 * Low level parsing function for .qst files.
 * Can only read the Blue Burst format.
 */
export function parse_qst(cursor: Cursor): ParseQstResult | undefined {
    // A .qst file contains two 88-byte headers that describe the embedded .dat and .bin files.
    let version = "PC";

    // Detect version.
    const version_a = cursor.u8();
    cursor.seek(1);
    const version_b = cursor.u8();

    if (version_a === 0x44) {
        version = "Dreamcast/GameCube";
    } else if (version_a === 0x58) {
        if (version_b === 0x44) {
            version = "Blue Burst";
        }
    } else if (version_a === 0xa6) {
        version = "Dreamcast download";
    }

    if (version === "Blue Burst") {
        // Read headers and contained files.
        cursor.seek_start(0);

        const headers = parse_headers(cursor);

        const files = parse_files(cursor, new Map(headers.map(h => [h.file_name, h.size])));

        for (const file of files) {
            const header = headers.find(h => h.file_name === file.name);

            if (header) {
                file.id = header.quest_id;
                file.name_2 = header.file_name_2;
            }
        }

        return {
            version,
            files,
        };
    } else {
        logger.error(`Can't parse ${version} QST files.`);
        return undefined;
    }
}

export type QstContainedFileParam = {
    id?: number;
    name: string;
    name_2?: string;
    data: ArrayBuffer;
};

export type WriteQstParams = {
    version?: string;
    files: QstContainedFileParam[];
};

/**
 * Always uses Blue Burst format.
 */
export function write_qst(params: WriteQstParams): ArrayBuffer {
    const files = params.files;
    const total_size = files
        .map(f => 88 + Math.ceil(f.data.byteLength / 1024) * 1056)
        .reduce((a, b) => a + b);
    const buffer = new ArrayBuffer(total_size);
    const cursor = new ArrayBufferCursor(buffer, Endianness.Little);

    write_file_headers(cursor, files);
    write_file_chunks(cursor, files);

    if (cursor.position !== total_size) {
        throw new Error(`Expected a final file size of ${total_size}, but got ${cursor.position}.`);
    }

    return buffer;
}

type QstHeader = {
    quest_id: number;
    file_name: string;
    file_name_2: string;
    size: number;
};

function parse_headers(cursor: Cursor): QstHeader[] {
    const headers: QstHeader[] = [];

    let prev_quest_id: number | undefined;
    let prev_file_name: string | undefined;

    for (let i = 0; i < 4; ++i) {
        cursor.seek(4);
        const quest_id = cursor.u16();
        cursor.seek(38);
        const file_name = cursor.string_ascii(16, true, true);
        const size = cursor.u32();
        // Not sure what this is:
        const file_name_2 = cursor.string_ascii(24, true, true);

        if (
            prev_quest_id != undefined &&
            prev_file_name != undefined &&
            (quest_id !== prev_quest_id || basename(file_name) !== basename(prev_file_name))
        ) {
            cursor.seek(-88);
            break;
        }

        prev_quest_id = quest_id;
        prev_file_name = file_name;

        headers.push({
            quest_id,
            file_name,
            file_name_2,
            size,
        });
    }

    return headers;
}

function parse_files(cursor: Cursor, expected_sizes: Map<string, number>): QstContainedFile[] {
    // Files are interleaved in 1056 byte chunks.
    // Each chunk has a 24 byte header, 1024 byte data segment and an 8 byte trailer.
    const files = new Map<
        string,
        {
            name: string;
            expected_size?: number;
            cursor: WritableCursor;
            chunk_nos: Set<number>;
        }
    >();

    while (cursor.bytes_left >= 1056) {
        const start_position = cursor.position;

        // Read meta data.
        const chunk_no = cursor.seek(4).u8();
        const file_name = cursor.seek(3).string_ascii(16, true, true);

        let file = files.get(file_name);

        if (!file) {
            const expected_size = expected_sizes.get(file_name);
            files.set(
                file_name,
                (file = {
                    name: file_name,
                    expected_size,
                    cursor: new ResizableBufferCursor(
                        new ResizableBuffer(expected_size || 10 * 1024),
                        Endianness.Little,
                    ),
                    chunk_nos: new Set(),
                }),
            );
        }

        if (file.chunk_nos.has(chunk_no)) {
            logger.warn(
                `File chunk number ${chunk_no} of file ${file_name} was already encountered, overwriting previous chunk.`,
            );
        } else {
            file.chunk_nos.add(chunk_no);
        }

        // Read file data.
        let size = cursor.seek(1024).u32();
        cursor.seek(-1028);

        if (size > 1024) {
            logger.warn(
                `Data segment size of ${size} is larger than expected maximum size, reading just 1024 bytes.`,
            );
            size = 1024;
        }

        const data = cursor.take(size);
        const chunk_position = chunk_no * 1024;
        file.cursor.size = Math.max(chunk_position + size, file.cursor.size);
        file.cursor.seek_start(chunk_position).write_cursor(data);

        // Skip the padding and the trailer.
        cursor.seek(1032 - data.size);

        if (cursor.position !== start_position + 1056) {
            throw new Error(
                `Read ${cursor.position -
                    start_position} file chunk message bytes instead of expected 1056.`,
            );
        }
    }

    if (cursor.bytes_left) {
        logger.warn(`${cursor.bytes_left} Bytes left in file.`);
    }

    for (const file of files.values()) {
        // Clean up file properties.
        file.cursor.seek_start(0);
        file.chunk_nos = new Set(Array.from(file.chunk_nos.values()).sort((a, b) => a - b));

        // Check whether the expected size was correct.
        if (file.expected_size != null && file.cursor.size !== file.expected_size) {
            logger.warn(
                `File ${file.name} has an actual size of ${file.cursor.size} instead of the expected size ${file.expected_size}.`,
            );
        }

        // Detect missing file chunks.
        const actual_size = Math.max(file.cursor.size, file.expected_size || 0);

        for (let chunk_no = 0; chunk_no < Math.ceil(actual_size / 1024); ++chunk_no) {
            if (!file.chunk_nos.has(chunk_no)) {
                logger.warn(`File ${file.name} is missing chunk ${chunk_no}.`);
            }
        }
    }

    const contained_files: QstContainedFile[] = [];

    for (const file of files.values()) {
        contained_files.push({
            name: file.name,
            data: file.cursor.seek_start(0).array_buffer(),
        });
    }

    return contained_files;
}

function write_file_headers(cursor: WritableCursor, files: QstContainedFileParam[]): void {
    for (const file of files) {
        if (file.name.length > 15) {
            throw new Error(`File ${file.name} has a name longer than 15 characters.`);
        }

        cursor.write_u16(88); // Header size.
        cursor.write_u16(0x44); // Magic number.
        cursor.write_u16(file.id || 0);

        for (let i = 0; i < 38; ++i) {
            cursor.write_u8(0);
        }

        cursor.write_string_ascii(file.name, 16);
        cursor.write_u32(file.data.byteLength);

        let file_name_2: string;

        if (file.name_2 == null) {
            // Not sure this makes sense.
            const dot_pos = file.name.lastIndexOf(".");
            file_name_2 =
                dot_pos === -1
                    ? file.name + "_j"
                    : file.name.slice(0, dot_pos) + "_j" + file.name.slice(dot_pos);
        } else {
            file_name_2 = file.name_2;
        }

        if (file_name_2.length > 24) {
            throw Error(
                `File ${file.name} has a file_name_2 length (${file_name_2}) longer than 24 characters.`,
            );
        }

        cursor.write_string_ascii(file_name_2, 24);
    }
}

function write_file_chunks(cursor: WritableCursor, files: QstContainedFileParam[]): void {
    // Files are interleaved in 1056 byte chunks.
    // Each chunk has a 24 byte header, 1024 byte data segment and an 8 byte trailer.
    const files_to_chunk = files.map(file => ({
        no: 0,
        data: new ArrayBufferCursor(file.data, Endianness.Little),
        name: file.name,
    }));
    let done = 0;

    while (done < files_to_chunk.length) {
        for (const file_to_chunk of files_to_chunk) {
            if (file_to_chunk.data.bytes_left) {
                if (
                    !write_file_chunk(
                        cursor,
                        file_to_chunk.data,
                        file_to_chunk.no++,
                        file_to_chunk.name,
                    )
                ) {
                    done++;
                }
            }
        }
    }

    for (const file_to_chunk of files_to_chunk) {
        const expected_chunks = Math.ceil(file_to_chunk.data.size / 1024);

        if (file_to_chunk.no !== expected_chunks) {
            throw new Error(
                `Expected to write ${expected_chunks} chunks for file "${file_to_chunk.name}" but ${file_to_chunk.no} where written.`,
            );
        }
    }
}

/**
 * @returns true if there are bytes left to write in data, false otherwise.
 */
function write_file_chunk(
    cursor: WritableCursor,
    data: Cursor,
    chunk_no: number,
    name: string,
): boolean {
    cursor.write_u8_array([28, 4, 19, 0]);
    cursor.write_u8(chunk_no);
    cursor.write_u8_array([0, 0, 0]);
    cursor.write_string_ascii(name, 16);

    const size = Math.min(1024, data.bytes_left);
    cursor.write_cursor(data.take(size));

    // Padding.
    for (let i = size; i < 1024; ++i) {
        cursor.write_u8(0);
    }

    cursor.write_u32(size);
    cursor.write_u32(0);

    return data.bytes_left > 0;
}
