import { Cursor } from "../../block/cursor/Cursor";
import { ResizableBlockCursor } from "../../block/cursor/ResizableBlockCursor";
import { WritableCursor } from "../../block/cursor/WritableCursor";
import { ResizableBlock } from "../../block/ResizableBlock";
import { LogManager } from "../../../logging";
import { browser_supports_webassembly } from "../../../util";
import { get_prs_wasm_module } from "./prs_wasm";
import { Result, ResultBuilder, success } from "../../../Result";
import { Severity } from "../../../Severity";

const logger = LogManager.get("core/data_formats/compression/prs/decompress");

const prs_wasm = get_prs_wasm_module();

/**
 * Automatically picks the best available decompression method.
 */
export function prs_decompress(cursor: Cursor): Result<Cursor> {
    try {
        if (browser_supports_webassembly() && prs_wasm) {
            return success(prs_wasm.prs_decompress_wasm(cursor));
        } else {
            return success(prs_decompress_js(cursor));
        }
    } catch (e) {
        return new ResultBuilder(logger)
            .add_problem(Severity.Error, "PRS-compressed stream is corrupt.", undefined, e)
            .failure();
    }
}

export function prs_decompress_js(cursor: Cursor): Cursor {
    const ctx = new Context(cursor);

    while (true) {
        if (ctx.read_flag_bit() === 1) {
            // Single byte copy.
            ctx.copy_u8();
        } else {
            // Multi byte copy.
            let length;
            let offset;

            if (ctx.read_flag_bit() === 0) {
                // Short copy.
                length = ctx.read_flag_bit() << 1;
                length |= ctx.read_flag_bit();
                length += 2;

                offset = ctx.read_u8() - 256;
            } else {
                // Long copy or end of file.
                offset = ctx.read_u16();

                // Two zero bytes implies that this is the end of the file.
                if (offset === 0) {
                    break;
                }

                // Do we need to read a length byte, or is it encoded in what we already have?
                length = offset & 0b111;
                offset >>>= 3;

                if (length === 0) {
                    length = ctx.read_u8();
                    length += 1;
                } else {
                    length += 2;
                }

                offset -= 8192;
            }

            ctx.offset_copy(offset, length);
        }
    }

    return ctx.dst.seek_start(0);
}

class Context {
    src: Cursor;
    dst: WritableCursor;
    flags: number;
    flag_bits_left: number;

    constructor(cursor: Cursor) {
        this.src = cursor;
        this.dst = new ResizableBlockCursor(
            new ResizableBlock(Math.floor(1.5 * cursor.size), cursor.endianness),
        );
        this.flags = 0;
        this.flag_bits_left = 0;
    }

    read_flag_bit(): number {
        // Fetch a new flag byte when the previous byte has been processed.
        if (this.flag_bits_left === 0) {
            this.flags = this.read_u8();
            this.flag_bits_left = 8;
        }

        const bit = this.flags & 1;
        this.flags >>>= 1;
        this.flag_bits_left -= 1;
        return bit;
    }

    copy_u8(): void {
        this.dst.write_u8(this.read_u8());
    }

    read_u8(): number {
        return this.src.u8();
    }

    read_u16(): number {
        return this.src.u16();
    }

    offset_copy(offset: number, length: number): void {
        if (offset < -8192 || offset > 0) {
            throw new Error(`offset was ${offset}, should be between -8192 and 0.`);
        }

        if (length < 1 || length > 256) {
            throw new Error(`length was ${length}, should be between 1 and 256.`);
        }

        // The length can be larger than -offset, in that case we copy -offset bytes size/-offset times.
        const buf_size = Math.min(-offset, length);

        this.dst.seek(offset);
        const buf = this.dst.take(buf_size);
        this.dst.seek(-offset - buf_size);

        for (let i = 0; i < Math.floor(length / buf_size); ++i) {
            this.dst.write_cursor(buf);
            buf.seek_start(0);
        }

        this.dst.write_cursor(buf.take(length % buf_size));
    }
}
