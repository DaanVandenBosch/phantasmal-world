/**
 * Resizable buffer.
 */
export class ResizableBuffer {
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
        return this._buffer.byteLength;
    }

    private _buffer: ArrayBuffer;

    get backing_buffer(): ArrayBuffer {
        return this._buffer;
    }

    private _data_view: DataView;

    get view(): DataView {
        return this._data_view;
    }

    constructor(initial_capacity: number = 8192) {
        this._buffer = new ArrayBuffer(initial_capacity);
        this._data_view = new DataView(this._buffer);
    }

    sub_view(offset: number, size: number): DataView {
        return new DataView(this._buffer, offset, size);
    }

    /**
     *  Increases buffer size if necessary.
     */
    private ensure_capacity(min_new_size: number): void {
        if (min_new_size > this.capacity) {
            let new_size = this.capacity || min_new_size;

            do {
                new_size *= 2;
            } while (new_size < min_new_size);

            const new_buffer = new ArrayBuffer(new_size);
            new Uint8Array(new_buffer).set(new Uint8Array(this._buffer, 0, this.size));
            this._buffer = new_buffer;
            this._data_view = new DataView(this._buffer);
        }
    }
}
