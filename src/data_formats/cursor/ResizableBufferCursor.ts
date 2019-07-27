import { Endianness } from "..";
import { ResizableBuffer } from "../ResizableBuffer";
import { AbstractWritableCursor } from "./AbstractWritableCursor";
import { WritableCursor } from "./WritableCursor";

export class ResizableBufferCursor extends AbstractWritableCursor implements WritableCursor {
    protected _size: number;

    get size(): number {
        return this._size;
    }

    set size(size: number) {
        if (size > this._size) {
            this.ensure_size(size - this._size);
        } else {
            this._size = size;
        }
    }

    protected buffer: ResizableBuffer;

    protected get backing_buffer(): ArrayBuffer {
        return this.buffer.backing_buffer;
    }

    protected get dv(): DataView {
        return this.buffer.view;
    }

    /**
     * @param buffer The buffer to read from.
     * @param endianness Decides in which byte order multi-byte integers and floats will be interpreted.
     * @param offset The start offset of the part that will be read from.
     * @param size The size of the part that will be read from.
     */
    constructor(
        buffer: ResizableBuffer,
        endianness: Endianness,
        offset: number = 0,
        size: number = buffer.size - offset
    ) {
        if (offset < 0 || offset > buffer.size) {
            throw new Error(`Offset ${offset} is out of bounds.`);
        }

        if (size < 0 || offset + size > buffer.size) {
            throw new Error(`Size ${size} is out of bounds.`);
        }

        super(endianness, offset);

        this.buffer = buffer;
        this._size = size;
    }

    take(size: number): ResizableBufferCursor {
        this.check_size("size", size, size);

        const offset = this.offset + this.position;
        const wrapper = new ResizableBufferCursor(this.buffer, this.endianness, offset, size);
        this._position += size;
        return wrapper;
    }

    protected ensure_size(size: number): void {
        const needed = this.position + size - this._size;

        if (needed > 0) {
            this._size += needed;

            if (this.buffer.size < this.offset + this._size) {
                this.buffer.size = this.offset + this._size;
            }
        }
    }
}
