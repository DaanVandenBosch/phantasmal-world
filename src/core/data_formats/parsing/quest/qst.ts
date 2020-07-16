import { Endianness } from "../../block/Endianness";
import { ArrayBufferCursor } from "../../block/cursor/ArrayBufferCursor";
import { Cursor } from "../../block/cursor/Cursor";
import { ResizableBlockCursor } from "../../block/cursor/ResizableBlockCursor";
import { WritableCursor } from "../../block/cursor/WritableCursor";
import { ResizableBlock } from "../../block/ResizableBlock";
import { assert, basename, defined } from "../../../util";
import { LogManager } from "../../../Logger";
import { Version } from "./Version";

const logger = LogManager.get("core/data_formats/parsing/quest/qst");

// .qst format
const DC_GC_PC_HEADER_SIZE = 60;
const BB_HEADER_SIZE = 88;
const ONLINE_QUEST = 0x44;
const DOWNLOAD_QUEST = 0xa6;

// Chunks
const CHUNK_BODY_SIZE = 1024;
const DC_GC_PC_CHUNK_HEADER_SIZE = 20;
const DC_GC_PC_CHUNK_TRAILER_SIZE = 4;
const DC_GC_PC_CHUNK_SIZE =
    CHUNK_BODY_SIZE + DC_GC_PC_CHUNK_HEADER_SIZE + DC_GC_PC_CHUNK_TRAILER_SIZE;
const BB_CHUNK_HEADER_SIZE = 24;
const BB_CHUNK_TRAILER_SIZE = 8;
const BB_CHUNK_SIZE = CHUNK_BODY_SIZE + BB_CHUNK_HEADER_SIZE + BB_CHUNK_TRAILER_SIZE;

export type QstContainedFile = {
    readonly id?: number;
    readonly filename: string;
    readonly quest_name?: string;
    readonly data: ArrayBuffer;
};

export type QstContent = {
    readonly version: Version;
    readonly online: boolean;
    readonly files: readonly QstContainedFile[];
};

/**
 * Low level parsing function for .qst files.
 * Can only read the Blue Burst format.
 */
export function parse_qst(cursor: Cursor): QstContent | undefined {
    // A .qst file contains two headers that describe the embedded .dat and .bin files.
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
                    header.filename
                } doesn't match the previous header's version ${Version[version]}.`,
            );
            return undefined;
        }

        if (online != undefined && header.online !== online) {
            logger.error(
                `Corrupt .qst file, header type ${
                    header.online ? '"online"' : '"download"'
                } for file ${header.filename} doesn't match the previous header's type ${
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

    const files = parse_files(cursor, version, new Map(headers.map(h => [h.filename, h])));

    return {
        version,
        online,
        files,
    };
}

export function write_qst({ version, online, files }: QstContent): ArrayBuffer {
    let file_header_size: number;
    let chunk_size: number;

    switch (version) {
        case Version.DC:
        case Version.GC:
        case Version.PC:
            file_header_size = DC_GC_PC_HEADER_SIZE;
            chunk_size = DC_GC_PC_CHUNK_SIZE;
            break;

        case Version.BB:
            file_header_size = BB_HEADER_SIZE;
            chunk_size = BB_CHUNK_SIZE;
            break;
    }

    const total_size = files
        .map(f => file_header_size + Math.ceil(f.data.byteLength / CHUNK_BODY_SIZE) * chunk_size)
        .reduce((a, b) => a + b);

    const buffer = new ArrayBuffer(total_size);
    const cursor = new ArrayBufferCursor(buffer, Endianness.Little);

    write_file_headers(cursor, files, version, online, file_header_size);
    write_file_chunks(cursor, files, version);

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
    readonly filename: string;
    readonly size: number;
};

function parse_headers(cursor: Cursor): QstHeader[] {
    const headers: QstHeader[] = [];

    let prev_quest_id: number | undefined = undefined;
    let prev_filename: string | undefined = undefined;

    // .qst files should have two headers, some malformed files have more.
    for (let i = 0; i < 4; ++i) {
        // Detect version and whether it's an online or download quest.
        let version: Version;
        let online: boolean;

        const version_a = cursor.u8();
        cursor.seek(1);
        const version_b = cursor.u8();
        cursor.seek(-3);

        if (version_a === BB_HEADER_SIZE && version_b === ONLINE_QUEST) {
            version = Version.BB;
            online = true;
        } else if (version_a === DC_GC_PC_HEADER_SIZE && version_b === ONLINE_QUEST) {
            version = Version.PC;
            online = true;
        } else if (version_b === DC_GC_PC_HEADER_SIZE) {
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
        let filename: string;
        let size: number;

        switch (version) {
            case Version.DC:
                cursor.seek(1); // Skip online/download.
                quest_id = cursor.u8();
                header_size = cursor.u16();
                name = cursor.string_ascii(32, true, true);
                cursor.seek(3);
                filename = cursor.string_ascii(16, true, true);
                cursor.seek(1);
                size = cursor.u32();
                break;

            case Version.GC:
                cursor.seek(1); // Skip online/download.
                quest_id = cursor.u8();
                header_size = cursor.u16();
                name = cursor.string_ascii(32, true, true);
                cursor.seek(4);
                filename = cursor.string_ascii(16, true, true);
                size = cursor.u32();
                break;

            case Version.PC:
                header_size = cursor.u16();
                cursor.seek(1); // Skip online/download.
                quest_id = cursor.u8();
                name = cursor.string_ascii(32, true, true);
                cursor.seek(4);
                filename = cursor.string_ascii(16, true, true);
                size = cursor.u32();
                break;

            case Version.BB:
                header_size = cursor.u16();
                cursor.seek(2); // Skip online/download.
                quest_id = cursor.u16();
                cursor.seek(38);
                filename = cursor.string_ascii(16, true, true);
                size = cursor.u32();
                name = cursor.string_ascii(24, true, true);
                break;
        }

        // Use some simple heuristics to figure out whether the file contains more than two headers.
        // Some malformed .qst files have extra headers.
        if (
            prev_quest_id != undefined &&
            prev_filename != undefined &&
            (quest_id !== prev_quest_id || basename(filename) !== basename(prev_filename))
        ) {
            cursor.seek(-header_size);
            break;
        }

        prev_quest_id = quest_id;
        prev_filename = filename;

        headers.push({
            version,
            online,
            quest_id,
            name,
            filename,
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
            chunk_size = DC_GC_PC_CHUNK_SIZE;
            trailer_size = DC_GC_PC_CHUNK_TRAILER_SIZE;
            break;

        case Version.BB:
            chunk_size = BB_CHUNK_SIZE;
            trailer_size = BB_CHUNK_TRAILER_SIZE;
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
                cursor: new ResizableBlockCursor(
                    new ResizableBlock(header?.size ?? 10 * CHUNK_BODY_SIZE, Endianness.Little),
                ),
                chunk_nos: new Set(),
            };
            files.set(file_name, file);
        }

        if (file.chunk_nos.has(chunk_no)) {
            logger.warn(
                `File chunk number ${chunk_no} of file ${file_name} was already encountered, overwriting previous chunk.`,
            );
        } else {
            file.chunk_nos.add(chunk_no);
        }

        // Read file data.
        let size = cursor.seek(CHUNK_BODY_SIZE).u32();
        cursor.seek(-CHUNK_BODY_SIZE - 4);

        if (size > CHUNK_BODY_SIZE) {
            logger.warn(
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
                `Read ${
                    cursor.position - start_position
                } file chunk message bytes instead of expected ${chunk_size}.`,
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
        const actual_size = Math.max(file.cursor.size, file.expected_size ?? 0);
        const expected_chunk_count = Math.ceil(actual_size / CHUNK_BODY_SIZE);

        for (let chunk_no = 0; chunk_no < expected_chunk_count; ++chunk_no) {
            if (!file.chunk_nos.has(chunk_no)) {
                logger.warn(`File ${file.name} is missing chunk ${chunk_no}.`);
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

function write_file_headers(
    cursor: WritableCursor,
    files: readonly QstContainedFile[],
    version: Version,
    online: boolean,
    header_size: number,
): void {
    let max_id: number;
    let max_quest_name_length: number;

    if (version === Version.BB) {
        max_id = 0xffff;
        max_quest_name_length = 23;
    } else {
        max_id = 0xff;
        max_quest_name_length = 31;
    }

    for (const file of files) {
        assert(
            file.id == undefined || (0 <= file.id && file.id <= max_id),
            () => `Quest ID should be between 0 and ${max_id}, inclusive.`,
        );
        assert(
            file.quest_name == undefined || file.quest_name.length <= max_quest_name_length,
            () =>
                `File ${file.filename} has a quest name longer than ${max_quest_name_length} characters (${file.quest_name}).`,
        );
        assert(
            file.filename.length <= 15,
            () => `File ${file.filename} has a filename longer than 15 characters.`,
        );

        switch (version) {
            case Version.DC:
                cursor.write_u8(online ? ONLINE_QUEST : DOWNLOAD_QUEST);
                cursor.write_u8(file.id ?? 0);
                cursor.write_u16(header_size);
                cursor.write_string_ascii(file.quest_name ?? file.filename, 32);
                cursor.write_u8(0);
                cursor.write_u8(0);
                cursor.write_u8(0);
                cursor.write_string_ascii(file.filename, 16);
                cursor.write_u8(0);
                cursor.write_u32(file.data.byteLength);
                break;

            case Version.GC:
                cursor.write_u8(online ? ONLINE_QUEST : DOWNLOAD_QUEST);
                cursor.write_u8(file.id ?? 0);
                cursor.write_u16(header_size);
                cursor.write_string_ascii(file.quest_name ?? file.filename, 32);
                cursor.write_u32(0);
                cursor.write_string_ascii(file.filename, 16);
                cursor.write_u32(file.data.byteLength);
                break;

            case Version.PC:
                cursor.write_u16(header_size);
                cursor.write_u8(online ? ONLINE_QUEST : DOWNLOAD_QUEST);
                cursor.write_u8(file.id ?? 0);
                cursor.write_string_ascii(file.quest_name ?? file.filename, 32);
                cursor.write_u32(0);
                cursor.write_string_ascii(file.filename, 16);
                cursor.write_u32(file.data.byteLength);
                break;

            case Version.BB:
                cursor.write_u16(header_size);
                cursor.write_u16(online ? ONLINE_QUEST : DOWNLOAD_QUEST);
                cursor.write_u16(file.id ?? 0);
                for (let i = 0; i < 38; i++) cursor.write_u8(0);
                cursor.write_string_ascii(file.filename, 16);
                cursor.write_u32(file.data.byteLength);
                cursor.write_string_ascii(file.quest_name ?? file.filename, 24);
                break;
        }
    }
}

function write_file_chunks(
    cursor: WritableCursor,
    files: readonly QstContainedFile[],
    version: Version,
): void {
    // Files are interleaved in chunks. Each chunk has a header, fixed-size data segment and a
    // trailer.
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
                        version,
                    )
                ) {
                    done++;
                }
            }
        }
    }

    for (const file_to_chunk of files_to_chunk) {
        const expected_chunks = Math.ceil(file_to_chunk.data.size / CHUNK_BODY_SIZE);

        if (file_to_chunk.no !== expected_chunks) {
            throw new Error(
                `Expected to write ${expected_chunks} chunks for file "${file_to_chunk.name}" but ${file_to_chunk.no} where written.`,
            );
        }
    }
}

/**
 * @returns true if there are bytes left to write in `data`, false otherwise.
 */
function write_file_chunk(
    cursor: WritableCursor,
    data: Cursor,
    chunk_no: number,
    name: string,
    version: Version,
): boolean {
    switch (version) {
        case Version.DC:
        case Version.GC:
            cursor.write_u8(0);
            cursor.write_u8(chunk_no);
            cursor.write_u16(0);
            break;

        case Version.PC:
            cursor.write_u8(0);
            cursor.write_u8(0);
            cursor.write_u8(0);
            cursor.write_u8(chunk_no);
            break;

        case Version.BB:
            cursor.write_u8_array([28, 4, 19, 0]);
            cursor.write_u32(chunk_no);
            break;
    }

    cursor.write_string_ascii(name, 16);

    const size = Math.min(CHUNK_BODY_SIZE, data.bytes_left);
    cursor.write_cursor(data.take(size));

    // Padding.
    for (let i = size; i < CHUNK_BODY_SIZE; ++i) {
        cursor.write_u8(0);
    }

    cursor.write_u32(size);

    if (version === Version.BB) {
        cursor.write_u32(0);
    }

    return data.bytes_left > 0;
}
