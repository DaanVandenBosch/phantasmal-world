import Logger from "js-logger";
import { Cursor } from "../../cursor/Cursor";
import { parse_iff } from "../iff";

const logger = Logger.get("data_formats/parsing/ninja/texture");

export type Xvm = {
    textures: Texture[];
};

export type Texture = {
    id: number;
    format: [number, number];
    width: number;
    height: number;
    size: number;
    data: ArrayBuffer;
};

type Header = {
    texture_count: number;
};

const XVMH = 0x484d5658;
const XVRT = 0x54525658;

export function parse_xvm(cursor: Cursor): Xvm {
    const chunks = parse_iff(cursor);
    const header_chunk = chunks.find(chunk => chunk.type === XVMH);
    const header = header_chunk && parse_header(header_chunk.data);

    const textures = chunks
        .filter(chunk => chunk.type === XVRT)
        .map(chunk => parse_texture(chunk.data));

    if (!header) {
        logger.warn("No header found.");
    } else if (header.texture_count !== textures.length) {
        logger.warn(
            `Found ${textures.length} textures instead of ${header.texture_count} as defined in the header.`
        );
    }

    return { textures };
}

function parse_header(cursor: Cursor): Header {
    const texture_count = cursor.u16();
    return {
        texture_count,
    };
}

function parse_texture(cursor: Cursor): Texture {
    const format_1 = cursor.u32();
    const format_2 = cursor.u32();
    const id = cursor.u32();
    const width = cursor.u16();
    const height = cursor.u16();
    const size = cursor.u32();
    cursor.seek(36);
    const data = cursor.array_buffer(size);
    return {
        id,
        format: [format_1, format_2],
        width,
        height,
        size,
        data,
    };
}
