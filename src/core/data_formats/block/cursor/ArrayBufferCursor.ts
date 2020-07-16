import { Endianness } from "../Endianness";
import { AbstractArrayBufferCursor } from "./AbstractArrayBufferCursor";

/**
 * A cursor for reading from an array buffer or part of an array buffer.
 */
export class ArrayBufferCursor extends AbstractArrayBufferCursor {
    private _size: number;

    get size(): number {
        return this._size;
    }

    set size(size: number) {
        if (size > this.backing_buffer.byteLength - this.offset) {
            throw new Error(`Size ${size} is out of bounds.`);
        }

        this._size = size;
    }

    protected backing_buffer: ArrayBuffer;
    protected dv: DataView;

    /**
     * @param buffer The buffer to read from.
     * @param endianness Decides in which byte order multi-byte integers and floats will be interpreted.
     * @param offset The start offset of the part that will be read from.
     * @param size The size of the part that will be read from.
     */
    constructor(
        buffer: ArrayBuffer,
        endianness: Endianness,
        offset: number = 0,
        size: number = buffer.byteLength - offset,
    ) {
        super(endianness, offset);
        this._size = size;
        this.backing_buffer = buffer;
        this.dv = new DataView(buffer, 0, buffer.byteLength);
    }

    take(size: number): ArrayBufferCursor {
        const offset = this.offset + this.position;
        const wrapper = new ArrayBufferCursor(this.backing_buffer, this.endianness, offset, size);
        this._position += size;
        return wrapper;
    }
}
