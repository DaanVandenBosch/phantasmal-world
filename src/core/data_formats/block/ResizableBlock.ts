import { Endianness } from "./Endianness";
import { AbstractWritableBlock } from "./AbstractWritableBlock";

/**
 * Resizable block backed by an ArrayBuffer which is reallocated when necessary.
 */
export class ResizableBlock extends AbstractWritableBlock {
    private _size: number = 0;

    get size(): number {
        return this._size;
    }

    set size(size: number) {
        if (size < 0) {
            throw new Error("Size should be non-negative.");
        }

        this.ensure_capacity(size);
        this._size = size;
    }

    get capacity(): number {
        return this.buffer.byteLength;
    }

    protected buffer: ArrayBuffer;
    protected data_view: DataView;

    constructor(initial_capacity: number = 8192, endianness: Endianness = Endianness.Little) {
        super(endianness);

        this.buffer = new ArrayBuffer(initial_capacity);
        this.data_view = new DataView(this.buffer);
    }

    /**
     * Reallocates the underlying ArrayBuffer if necessary.
     */
    private ensure_capacity(min_new_size: number): void {
        if (min_new_size > this.capacity) {
            let new_size = this.capacity || min_new_size;

            do {
                new_size *= 2;
            } while (new_size < min_new_size);

            const new_buffer = new ArrayBuffer(new_size);
            new Uint8Array(new_buffer).set(new Uint8Array(this.buffer, 0, this.size));
            this.buffer = new_buffer;
            this.data_view = new DataView(this.buffer);
        }
    }
}
