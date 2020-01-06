import { Cursor } from "../../cursor/Cursor";
import { parse_iff, parse_iff_headers } from "../iff";
import { LogManager } from "../../../Logger";
import { Result, result_builder } from "../../../Result";
import { Severity } from "../../../Severity";

const logger = LogManager.get("core/data_formats/parsing/ninja/texture");

export type Xvm = {
    textures: XvrTexture[];
};

export type XvrTexture = {
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

export function parse_xvr(cursor: Cursor): XvrTexture {
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

export function is_xvm(cursor: Cursor): boolean {
    const iff_result = parse_iff_headers(cursor, true);
    cursor.seek_start(0);

    return (
        iff_result.success &&
        iff_result.value.find(chunk => chunk.type === XVMH || chunk.type === XVRT) != undefined
    );
}

export function parse_xvm(cursor: Cursor): Result<Xvm> {
    const iff_result = parse_iff(cursor);

    if (!iff_result.success) {
        return iff_result;
    }

    const result = result_builder<Xvm>(logger);
    result.add_result(iff_result);
    const chunks = iff_result.value;
    const header_chunk = chunks.find(chunk => chunk.type === XVMH);
    const header = header_chunk && parse_header(header_chunk.data);

    const textures = chunks
        .filter(chunk => chunk.type === XVRT)
        .map(chunk => parse_xvr(chunk.data));

    if (!header && textures.length === 0) {
        result.add_problem(
            Severity.Error,
            "Corrupted XVM file.",
            "No header and no XVRT chunks found.",
        );

        return result.failure();
    }

    if (header && header.texture_count !== textures.length) {
        result.add_problem(
            Severity.Warning,
            "Corrupted XVM file.",
            `Found ${textures.length} textures instead of ${header.texture_count} as defined in the header.`,
        );
    }

    return result.success({ textures });
}

function parse_header(cursor: Cursor): Header {
    const texture_count = cursor.u16();
    return {
        texture_count,
    };
}
