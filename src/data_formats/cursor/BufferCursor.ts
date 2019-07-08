import { ArrayBufferCursor } from "./ArrayBufferCursor";
import { Endianness } from "..";

export class BufferCursor extends ArrayBufferCursor {
    /**
     * @param buffer The buffer to read from.
     * @param endianness Decides in which byte order multi-byte integers and floats will be interpreted.
     * @param offset The start offset of the part that will be read from.
     * @param size The size of the part that will be read from.
     */
    constructor(
        buffer: Buffer,
        endianness: Endianness,
        offset: number = 0,
        size: number = buffer.byteLength
    ) {
        if (offset < 0 || offset > buffer.byteLength) {
            throw new Error(`Offset ${offset} is out of bounds.`);
        }

        if (size < 0 || size > buffer.byteLength - offset) {
            throw new Error(`Size ${size} is out of bounds.`);
        }

        super(buffer.buffer, endianness, buffer.byteOffset + offset, size);
    }
}
