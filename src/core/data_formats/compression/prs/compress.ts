import { Endianness } from "../../block/Endianness";
import { Cursor } from "../../block/cursor/Cursor";
import { ResizableBlockCursor } from "../../block/cursor/ResizableBlockCursor";
import { WritableCursor } from "../../block/cursor/WritableCursor";
import { ResizableBlock } from "../../block/ResizableBlock";
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
    const comp_cursor = cursor.take(cursor.size);
    cursor.seek_start(0);

    while (cursor.bytes_left) {
        // Find the longest match.
        let best_offset = 0;
        let best_size = 0;
        const start_pos = cursor.position;
        const min_offset = Math.max(0, start_pos - Math.min(0x800, cursor.bytes_left));

        for (let i = start_pos - 255; i >= min_offset; i--) {
            comp_cursor.seek_start(i);
            let size = 0;

            while (cursor.bytes_left && size <= 254 && cursor.u8() === comp_cursor.u8()) {
                size++;
            }

            cursor.seek_start(start_pos);

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
        this.output = new ResizableBlockCursor(new ResizableBlock(capacity, endianness));
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
