import { Endianness } from "..";
import { AbstractCursor } from "./AbstractCursor";
import { Cursor } from "./Cursor";

export class BufferCursor extends AbstractCursor implements Cursor {
    readonly size: number;

    protected buffer: Buffer;

    protected get backing_buffer(): ArrayBuffer {
        return this.buffer.buffer;
    }

    protected dv: DataView;

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
        size: number = buffer.byteLength - offset
    ) {
        if (offset < 0 || offset > buffer.byteLength) {
            throw new Error(`Offset ${offset} is out of bounds.`);
        }

        if (size < 0 || size > buffer.byteLength - offset) {
            throw new Error(`Size ${size} is out of bounds.`);
        }

        super(endianness, buffer.byteOffset + offset);

        this.buffer = buffer;
        this.size = size;
        this.dv = new DataView(buffer.buffer, 0, buffer.buffer.byteLength);
    }

    take(size: number): BufferCursor {
        const offset = this.offset + this.position;
        const wrapper = new BufferCursor(this.buffer, this.endianness, offset, size);
        this._position += size;
        return wrapper;
    }
}
