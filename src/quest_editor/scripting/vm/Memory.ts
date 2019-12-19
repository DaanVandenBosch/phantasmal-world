import { ArrayBufferCursor } from "../../../core/data_formats/cursor/ArrayBufferCursor";
import { Endianness } from "../../../core/data_formats/Endianness";

export class Memory extends ArrayBufferCursor {
    constructor(size: number, endianness: Endianness) {
        super(new ArrayBuffer(size), endianness);
    }

    public zero(): void {
        new Uint32Array(this.backing_buffer).fill(0);
    }
}
