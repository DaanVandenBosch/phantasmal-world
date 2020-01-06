import { Endianness } from "../../Endianness";
import { ArrayBufferCursor } from "../../cursor/ArrayBufferCursor";
import { Cursor } from "../../cursor/Cursor";
import { ResizableBufferCursor } from "../../cursor/ResizableBufferCursor";
import { WritableCursor } from "../../cursor/WritableCursor";
import { ResizableBuffer } from "../../ResizableBuffer";
import { basename, defined } from "../../../util";
import { LogManager } from "../../../Logger";
import { Version } from "./Version";

const logger = LogManager.get("core/data_formats/parsing/quest/qst");

const BB_HEADER_SIZE = 88;
const PC_GC_HEADER_SIZE = 60;
const ONLINE_QUEST = 0x44;
const DOWNLOAD_QUEST = 0xa6;
const CHUNK_BODY_SIZE = 1024;

export type QstContainedFile = {
    readonly id?: number;
    readonly filename: string;
    readonly quest_name?: string;
    readonly data: ArrayBuffer;
};

export type ParseQstResult = {
    readonly version: Version;
    readonly online: boolean;
    readonly files: readonly QstContainedFile[];
};

/**
 * Low level parsing function for .qst files.
 * Can only read the Blue Burst format.
 */
export function parse_qst(cursor: Cursor): ParseQstResult | undefined {
    // A .qst file contains two 88-byte headers that describe the embedded .dat and .bin files.
    // Read headers and contained files.
    const headers = parse_headers(cursor);

    if (headers.length < 2) {
        logger.error(
            `Corrupt .qst file, expected at least 2 headers but only found ${headers.length}.`,
        );
        return undefined;
    }

    let version: Version | undefined = undefined;
    let online: boolean | undefined = undefined;

    for (const header of headers) {
        if (version != undefined && header.version !== version) {
            logger.error(
                `Corrupt .qst file, header version ${Version[header.version]} for file ${
                    header.file_name
                } doesn't match the previous header's version ${Version[version]}.`,
            );
            return undefined;
        }

        if (online != undefined && header.online !== online) {
            logger.error(
                `Corrupt .qst file, header type ${
                    header.online ? '"online"' : '"download"'
                } for file ${header.file_name} doesn't match the previous header's type ${
                    online ? '"online"' : '"download"'
                }.`,
            );
            return undefined;
        }

        version = header.version;
        online = header.online;
    }

    defined(version, "version");
    defined(online, "online");

    const files = parse_files(cursor, version, new Map(headers.map(h => [h.file_name, h])));

    return {
        version,
        online,
        files,
    };
}

export type QstContainedFileParam = {
    readonly id?: number;
    readonly filename: string;
    readonly quest_name?: string;
    readonly data: ArrayBuffer;
};

export type WriteQstParams = {
    readonly version?: Version;
    readonly files: readonly QstContainedFileParam[];
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
    readonly version: Version;
    readonly online: boolean;
    readonly quest_id: number;
    readonly name: string;
    readonly file_name: string;
    readonly size: number;
};

function parse_headers(cursor: Cursor): QstHeader[] {
    const headers: QstHeader[] = [];

    let prev_quest_id: number | undefined = undefined;
    let prev_file_name: string | undefined = undefined;

    // .qst files should have two headers, some malformed files have more.
    for (let i = 0; i < 4; ++i) {
        // Detect version and whether it's an online or download quest.
        let version;
        let online;

        const version_a = cursor.u8();
        cursor.seek(1);
        const version_b = cursor.u8();
        cursor.seek(-3);

        if (version_a === BB_HEADER_SIZE && version_b === ONLINE_QUEST) {
            version = Version.BB;
            online = true;
        } else if (version_a === PC_GC_HEADER_SIZE && version_b === ONLINE_QUEST) {
            version = Version.PC;
            online = true;
        } else if (version_b === PC_GC_HEADER_SIZE) {
            const pos = cursor.position;
            cursor.seek(35);

            if (cursor.u8() === 0) {
                version = Version.GC;
            } else {
                version = Version.DC;
            }

            cursor.seek_start(pos);

            if (version_a === ONLINE_QUEST) {
                online = true;
            } else if (version_a === DOWNLOAD_QUEST) {
                online = false;
            } else {
                break;
            }
        } else {
            break;
        }

        // Read header.
        let header_size;
        let quest_id: number;
        let name: string;
        let file_name: string;
        let size: number;

        switch (version) {
            case Version.DC:
                cursor.seek(1); // Skip online/download.
                quest_id = cursor.u8();
                header_size = cursor.u16();
                name = cursor.string_ascii(32, true, true);
                cursor.seek(3);
                file_name = cursor.string_ascii(16, true, true);
                size = cursor.u32();
                break;

            case Version.GC:
                cursor.seek(1); // Skip online/download.
                quest_id = cursor.u8();
                header_size = cursor.u16();
                name = cursor.string_ascii(32, true, true);
                cursor.seek(4);
                file_name = cursor.string_ascii(16, true, true);
                size = cursor.u32();
                break;

            case Version.PC:
                header_size = cursor.u16();
                cursor.seek(1); // Skip online/download.
                quest_id = cursor.u8();
                name = cursor.string_ascii(32, true, true);
                cursor.seek(4);
                file_name = cursor.string_ascii(16, true, true);
                size = cursor.u32();
                break;

            case Version.BB:
                header_size = cursor.u16();
                cursor.seek(2); // Skip online/download.
                quest_id = cursor.u16();
                cursor.seek(38);
                file_name = cursor.string_ascii(16, true, true);
                size = cursor.u32();
                name = cursor.string_ascii(24, true, true);
                break;
        }

        // Use some simple heuristics to figure out whether the file contains more than two headers.
        // Some malformed .qst files have extra headers.
        if (
            prev_quest_id != undefined &&
            prev_file_name != undefined &&
            (quest_id !== prev_quest_id || basename(file_name) !== basename(prev_file_name))
        ) {
            cursor.seek(-header_size);
            break;
        }

        prev_quest_id = quest_id;
        prev_file_name = file_name;

        headers.push({
            version,
            online,
            quest_id,
            name,
            file_name,
            size,
        });
    }

    return headers;
}

function parse_files(
    cursor: Cursor,
    version: Version,
    headers: Map<string, QstHeader>,
): QstContainedFile[] {
    // Files are interleaved in 1056 byte chunks.
    // Each chunk has a 20 or 24 byte header, 1024 byte data segment and an 4 or 8 byte trailer.
    const files = new Map<
        string,
        {
            name: string;
            expected_size?: number;
            cursor: WritableCursor;
            chunk_nos: Set<number>;
        }
    >();

    let chunk_size: number; // Size including padding, header and trailer.
    let trailer_size: number;

    switch (version) {
        case Version.DC:
        case Version.GC:
        case Version.PC:
            chunk_size = CHUNK_BODY_SIZE + 24;
            trailer_size = 4;
            break;

        case Version.BB:
            chunk_size = CHUNK_BODY_SIZE + 32;
            trailer_size = 8;
            break;
    }

    while (cursor.bytes_left >= chunk_size) {
        const start_position = cursor.position;

        // Read chunk header.
        let chunk_no: number;

        switch (version) {
            case Version.DC:
            case Version.GC:
                cursor.seek(1);
                chunk_no = cursor.u8();
                cursor.seek(2);
                break;

            case Version.PC:
                cursor.seek(3);
                chunk_no = cursor.u8();
                break;

            case Version.BB:
                cursor.seek(4);
                chunk_no = cursor.u32();
                break;
        }

        const file_name = cursor.string_ascii(16, true, true);
        let file = files.get(file_name);

        if (!file) {
            const header = headers.get(file_name);
            file = {
                name: file_name,
                expected_size: header?.size,
                cursor: new ResizableBufferCursor(
                    new ResizableBuffer(header?.size ?? 10 * 1024),
                    Endianness.Little,
                ),
                chunk_nos: new Set(),
            };
            files.set(file_name, file);
        }

        if (file.chunk_nos.has(chunk_no)) {
            logger.warning(
                `File chunk number ${chunk_no} of file ${file_name} was already encountered, overwriting previous chunk.`,
            );
        } else {
            file.chunk_nos.add(chunk_no);
        }

        // Read file data.
        let size = cursor.seek(CHUNK_BODY_SIZE).u32();
        cursor.seek(-CHUNK_BODY_SIZE - 4);

        if (size > CHUNK_BODY_SIZE) {
            logger.warning(
                `Data segment size of ${size} is larger than expected maximum size, reading just ${CHUNK_BODY_SIZE} bytes.`,
            );
            size = CHUNK_BODY_SIZE;
        }

        const data = cursor.take(size);
        const chunk_position = chunk_no * CHUNK_BODY_SIZE;
        file.cursor.size = Math.max(chunk_position + size, file.cursor.size);
        file.cursor.seek_start(chunk_position).write_cursor(data);

        // Skip the padding and the trailer.
        cursor.seek(CHUNK_BODY_SIZE + trailer_size - data.size);

        if (cursor.position !== start_position + chunk_size) {
            throw new Error(
                `Read ${cursor.position -
                    start_position} file chunk message bytes instead of expected ${chunk_size}.`,
            );
        }
    }

    if (cursor.bytes_left) {
        logger.warning(`${cursor.bytes_left} Bytes left in file.`);
    }

    for (const file of files.values()) {
        // Clean up file properties.
        file.cursor.seek_start(0);
        file.chunk_nos = new Set(Array.from(file.chunk_nos.values()).sort((a, b) => a - b));

        // Check whether the expected size was correct.
        if (file.expected_size != null && file.cursor.size !== file.expected_size) {
            logger.warning(
                `File ${file.name} has an actual size of ${file.cursor.size} instead of the expected size ${file.expected_size}.`,
            );
        }

        // Detect missing file chunks.
        const actual_size = Math.max(file.cursor.size, file.expected_size ?? 0);
        const expected_chunk_count = Math.ceil(actual_size / CHUNK_BODY_SIZE);

        for (let chunk_no = 0; chunk_no < expected_chunk_count; ++chunk_no) {
            if (!file.chunk_nos.has(chunk_no)) {
                logger.warning(`File ${file.name} is missing chunk ${chunk_no}.`);
            }
        }
    }

    const contained_files: QstContainedFile[] = [];

    for (const file of files.values()) {
        const header = headers.get(file.name);
        contained_files.push({
            id: header?.quest_id,
            filename: file.name,
            quest_name: header?.name,
            data: file.cursor.seek_start(0).array_buffer(),
        });
    }

    return contained_files;
}

function write_file_headers(cursor: WritableCursor, files: readonly QstContainedFileParam[]): void {
    for (const file of files) {
        if (file.filename.length > 15) {
            throw new Error(`File ${file.filename} has a name longer than 15 characters.`);
        }

        cursor.write_u16(88); // Header size.
        cursor.write_u16(0x44); // Magic number.
        cursor.write_u16(file.id || 0);

        for (let i = 0; i < 38; ++i) {
            cursor.write_u8(0);
        }

        cursor.write_string_ascii(file.filename, 16);
        cursor.write_u32(file.data.byteLength);

        let file_name_2: string;

        if (file.quest_name == null) {
            // Not sure this makes sense.
            const dot_pos = file.filename.lastIndexOf(".");
            file_name_2 =
                dot_pos === -1
                    ? file.filename + "_j"
                    : file.filename.slice(0, dot_pos) + "_j" + file.filename.slice(dot_pos);
        } else {
            file_name_2 = file.quest_name;
        }

        if (file_name_2.length > 24) {
            throw Error(
                `File ${file.filename} has a file_name_2 length (${file_name_2}) longer than 24 characters.`,
            );
        }

        cursor.write_string_ascii(file_name_2, 24);
    }
}

function write_file_chunks(cursor: WritableCursor, files: readonly QstContainedFileParam[]): void {
    // Files are interleaved in 1056 byte chunks.
    // Each chunk has a 24 byte header, 1024 byte data segment and an 8 byte trailer.
    const files_to_chunk = files.map(file => ({
        no: 0,
        data: new ArrayBufferCursor(file.data, Endianness.Little),
        name: file.filename,
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
