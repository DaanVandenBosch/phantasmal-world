import { Endianness } from "../../Endianness";
import { Cursor } from "../../cursor/Cursor";
import { ResizableBufferCursor } from "../../cursor/ResizableBufferCursor";
import { WritableCursor } from "../../cursor/WritableCursor";
import { ResizableBuffer } from "../../ResizableBuffer";
import { browser_supports_webassembly } from "../../../util";
import { get_prs_wasm_module } from "./prs_wasm";

const prs_wasm = get_prs_wasm_module();

/**
 * Automatically picks the best available compression method.
 */
export function prs_compress(cursor: Cursor): Cursor {
    if (browser_supports_webassembly() && prs_wasm) {
        return prs_wasm.prs_compress_wasm(cursor);
    } else {
        return prs_compress_js(cursor);
    }
}

export function prs_compress_js(cursor: Cursor): Cursor {
    const pc = new Context(cursor.size, cursor.endianness);

    while (cursor.bytes_left) {
        // Find the longest match.
        let best_offset = 0;
        let best_size = 0;
        const min_offset = Math.max(0, cursor.position - Math.min(0x800, cursor.bytes_left));

        for (let i = cursor.position - 255; i >= min_offset; i--) {
            let s1 = cursor.position;
            let s2 = i;
            let size = 0;

            // Optimization: compare 4 bytes at a time while we can.
            while (s1 + 3 < cursor.size && size < 252 && cursor.u32_at(s1) === cursor.u32_at(s2)) {
                size += 4;
                s1 += 4;
                s2 += 4;
            }

            while (s1 < cursor.size && size < 255 && cursor.u8_at(s1) === cursor.u8_at(s2)) {
                size++;
                s1++;
                s2++;
            }

            if (size >= best_size) {
                best_offset = i;
                best_size = size;

                if (size >= 255) {
                    break;
                }
            }
        }

        if (best_size < 3) {
            pc.add_u8(cursor.u8());
        } else {
            pc.copy(best_offset - cursor.position, best_size);
            cursor.seek(best_size);
        }
    }

    return pc.finalize();
}

class Context {
    private readonly output: WritableCursor;
    private flags = 0;
    private flag_bits_left = 0;
    private flag_offset = 0;

    constructor(capacity: number, endianness: Endianness) {
        this.output = new ResizableBufferCursor(new ResizableBuffer(capacity), endianness);
    }

    add_u8(value: number): void {
        this.write_control_bit(1);
        this.write_u8(value);
    }

    copy(offset: number, size: number): void {
        if (offset > -256 && size <= 5) {
            this.short_copy(offset, size);
        } else {
            this.long_copy(offset, size);
        }
    }

    finalize(): Cursor {
        this.write_control_bit(0);
        this.write_control_bit(1);

        this.flags >>>= this.flag_bits_left;
        const pos = this.output.position;
        this.output.seek_start(this.flag_offset).write_u8(this.flags).seek_start(pos);

        this.write_u8(0);
        this.write_u8(0);
        return this.output.seek_start(0);
    }

    private write_control_bit(bit: number): void {
        if (this.flag_bits_left-- === 0) {
            // Write out the flags to their position in the file, and store the next flags byte position.
            const pos = this.output.position;
            this.output.seek_start(this.flag_offset);
            this.output.write_u8(this.flags);
            this.output.seek_start(pos);
            this.output.write_u8(0); // Placeholder for the next flags byte.
            this.flag_offset = pos;
            this.flag_bits_left = 7;
        }

        this.flags >>>= 1;

        if (bit) {
            this.flags |= 0x80;
        }
    }

    private write_u8(data: number): void {
        this.output.write_u8(data);
    }

    private short_copy(offset: number, size: number): void {
        size -= 2;
        this.write_control_bit(0);
        this.write_control_bit(0);
        this.write_control_bit((size >>> 1) & 1);
        this.write_control_bit(size & 1);
        this.write_u8(offset & 0xff);
    }

    private long_copy(offset: number, size: number): void {
        if (size <= 9) {
            this.write_control_bit(0);
            this.write_control_bit(1);
            this.write_u8(((offset << 3) & 0xf8) | ((size - 2) & 0x07));
            this.write_u8((offset >> 5) & 0xff);
        } else {
            this.write_control_bit(0);
            this.write_control_bit(1);
            this.write_u8((offset << 3) & 0xf8);
            this.write_u8((offset >> 5) & 0xff);
            this.write_u8(size - 1);
        }
    }
}
