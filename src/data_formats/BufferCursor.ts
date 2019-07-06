// TODO: remove dependency on text-encoding because it is no longer maintained.
import { TextDecoder, TextEncoder } from "text-encoding";

const ASCII_DECODER = new TextDecoder("ascii");
const UTF_16BE_DECODER = new TextDecoder("utf-16be");
const UTF_16LE_DECODER = new TextDecoder("utf-16le");

const ASCII_ENCODER = new TextEncoder("ascii");
const UTF_16BE_ENCODER = new TextEncoder("utf-16be");
const UTF_16LE_ENCODER = new TextEncoder("utf-16le");

/**
 * A cursor for reading and writing binary data.
 * Uses an ArrayBuffer internally. This buffer is reallocated if and only if a write beyond the current capacity happens.
 */
export class BufferCursor {
    private _size: number = 0;

    /**
     * The cursor's size. This value will always be non-negative and equal to or smaller than the cursor's capacity.
     */
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

    private _position: number;

    /**
     * The position from where bytes will be read or written.
     */
    get position(): number {
        return this._position;
    }

    private _little_endian: boolean = false;

    /**
     * Byte order mode.
     */
    get little_endian(): boolean {
        return this._little_endian;
    }

    set little_endian(little_endian: boolean) {
        this._little_endian = little_endian;
        this.utf16_decoder = little_endian ? UTF_16LE_DECODER : UTF_16BE_DECODER;
        this.utf16_encoder = little_endian ? UTF_16LE_ENCODER : UTF_16BE_ENCODER;
    }

    /**
     * The amount of bytes left to read from the current position onward.
     */
    get bytes_left(): number {
        return this.size - this.position;
    }

    /**
     * The size of the underlying buffer. This value will always be equal to or greater than the cursor's size.
     */
    get capacity(): number {
        return this.buffer.byteLength;
    }

    private _buffer: ArrayBuffer;

    get buffer(): ArrayBuffer {
        return this._buffer;
    }

    private dv: DataView;
    private utf16_decoder: TextDecoder = UTF_16BE_DECODER;
    private utf16_encoder: TextEncoder = UTF_16BE_ENCODER;

    /**
     * @param buffer_or_capacity - If an ArrayBuffer or Buffer is given, writes to the cursor will be reflected in this buffer and vice versa until a cursor write that requires allocating a new internal buffer happens.
     * @param little_endian - Decides in which byte order multi-byte integers and floats will be interpreted.
     */
    constructor(buffer_or_capacity: ArrayBuffer | Buffer | number, little_endian: boolean = false) {
        if (typeof buffer_or_capacity === "number") {
            this._buffer = new ArrayBuffer(buffer_or_capacity);
            this.size = 0;
        } else if (buffer_or_capacity instanceof ArrayBuffer) {
            this._buffer = buffer_or_capacity;
            this.size = buffer_or_capacity.byteLength;
        } else if (buffer_or_capacity instanceof Buffer) {
            // Use the backing ArrayBuffer.
            this._buffer = buffer_or_capacity.buffer;
            this.size = buffer_or_capacity.byteLength;
        } else {
            throw new Error("buffer_or_capacity should be an ArrayBuffer, a Buffer or a number.");
        }

        this.little_endian = little_endian;
        this._position = 0;
        this.dv = new DataView(this.buffer);
    }

    /**
     * Seek forward or backward by a number of bytes.
     *
     * @param offset - if positive, seeks forward by offset bytes, otherwise seeks backward by -offset bytes.
     */
    seek(offset: number): BufferCursor {
        return this.seek_start(this.position + offset);
    }

    /**
     * Seek forward from the start of the cursor by a number of bytes.
     *
     * @param offset - greater or equal to 0 and smaller than size
     */
    seek_start(offset: number): BufferCursor {
        if (offset < 0 || offset > this.size) {
            throw new Error(`Offset ${offset} is out of bounds.`);
        }

        this._position = offset;
        return this;
    }

    /**
     * Seek backward from the end of the cursor by a number of bytes.
     *
     * @param offset - greater or equal to 0 and smaller than size
     */
    seek_end(offset: number): BufferCursor {
        if (offset < 0 || offset > this.size) {
            throw new Error(`Offset ${offset} is out of bounds.`);
        }

        this._position = this.size - offset;
        return this;
    }

    /**
     * Reads an unsigned 8-bit integer and increments position by 1.
     */
    u8(): number {
        return this.dv.getUint8(this._position++);
    }

    /**
     * Reads an unsigned 16-bit integer and increments position by 2.
     */
    u16(): number {
        const r = this.dv.getUint16(this.position, this.little_endian);
        this._position += 2;
        return r;
    }

    /**
     * Reads an unsigned 32-bit integer and increments position by 4.
     */
    u32(): number {
        const r = this.dv.getUint32(this.position, this.little_endian);
        this._position += 4;
        return r;
    }

    /**
     * Reads an signed 8-bit integer and increments position by 1.
     */
    i8(): number {
        return this.dv.getInt8(this._position++);
    }

    /**
     * Reads a signed 16-bit integer and increments position by 2.
     */
    i16(): number {
        const r = this.dv.getInt16(this.position, this.little_endian);
        this._position += 2;
        return r;
    }

    /**
     * Reads a signed 32-bit integer and increments position by 4.
     */
    i32(): number {
        const r = this.dv.getInt32(this.position, this.little_endian);
        this._position += 4;
        return r;
    }

    /**
     * Reads a 32-bit floating point number and increments position by 4.
     */
    f32(): number {
        const r = this.dv.getFloat32(this.position, this.little_endian);
        this._position += 4;
        return r;
    }

    /**
     * Reads n unsigned 8-bit integers and increments position by n.
     */
    u8_array(n: number): number[] {
        const array = [];
        for (let i = 0; i < n; ++i) array.push(this.dv.getUint8(this._position++));
        return array;
    }

    /**
     * Reads n unsigned 16-bit integers and increments position by 2n.
     */
    u16_array(n: number): number[] {
        const array = [];

        for (let i = 0; i < n; ++i) {
            array.push(this.dv.getUint16(this.position, this.little_endian));
            this._position += 2;
        }

        return array;
    }

    /**
     * Reads n unsigned 32-bit integers and increments position by 4n.
     */
    u32_array(n: number): number[] {
        const array = [];

        for (let i = 0; i < n; ++i) {
            array.push(this.dv.getUint32(this.position, this.little_endian));
            this._position += 4;
        }

        return array;
    }

    /**
     * Consumes a variable number of bytes.
     *
     * @param size - the amount bytes to consume.
     * @returns a new cursor containing size bytes.
     */
    take(size: number): BufferCursor {
        if (size < 0 || size > this.size - this.position) {
            throw new Error(`Size ${size} out of bounds.`);
        }

        this._position += size;
        return new BufferCursor(
            this.buffer.slice(this.position - size, this.position),
            this.little_endian
        );
    }

    /**
     * Consumes up to maxByteLength bytes.
     */
    string_ascii(
        max_byte_length: number,
        null_terminated: boolean,
        drop_remaining: boolean
    ): string {
        const string_length = null_terminated
            ? this.index_of_u8(0, max_byte_length) - this.position
            : max_byte_length;

        const r = ASCII_DECODER.decode(new DataView(this.buffer, this.position, string_length));
        this._position += drop_remaining
            ? max_byte_length
            : Math.min(string_length + 1, max_byte_length);
        return r;
    }

    /**
     * Consumes up to maxByteLength bytes.
     */
    string_utf16(
        max_byte_length: number,
        null_terminated: boolean,
        drop_remaining: boolean
    ): string {
        const string_length = null_terminated
            ? this.index_of_u16(0, max_byte_length) - this.position
            : Math.floor(max_byte_length / 2) * 2;

        const r = this.utf16_decoder.decode(
            new DataView(this.buffer, this.position, string_length)
        );
        this._position += drop_remaining
            ? max_byte_length
            : Math.min(string_length + 2, max_byte_length);
        return r;
    }

    /**
     * Writes an unsigned 8-bit integer and increments position by 1. If necessary, grows the cursor and reallocates the underlying buffer.
     */
    write_u8(value: number): BufferCursor {
        this.ensure_capacity(this.position + 1);

        this.dv.setUint8(this._position++, value);

        if (this.position > this.size) {
            this.size = this.position;
        }

        return this;
    }

    /**
     * Writes an unsigned 16-bit integer and increments position by 2. If necessary, grows the cursor and reallocates the underlying buffer.
     */
    write_u16(value: number): BufferCursor {
        this.ensure_capacity(this.position + 2);

        this.dv.setUint16(this.position, value, this.little_endian);
        this._position += 2;

        if (this.position > this.size) {
            this.size = this.position;
        }

        return this;
    }

    /**
     * Writes an unsigned 32-bit integer and increments position by 4. If necessary, grows the cursor and reallocates the underlying buffer.
     */
    write_u32(value: number): BufferCursor {
        this.ensure_capacity(this.position + 4);

        this.dv.setUint32(this.position, value, this.little_endian);
        this._position += 4;

        if (this.position > this.size) {
            this.size = this.position;
        }

        return this;
    }

    /**
     * Writes a signed 32-bit integer and increments position by 4. If necessary, grows the cursor and reallocates the underlying buffer.
     */
    write_i32(value: number): BufferCursor {
        this.ensure_capacity(this.position + 4);

        this.dv.setInt32(this.position, value, this.little_endian);
        this._position += 4;

        if (this.position > this.size) {
            this.size = this.position;
        }

        return this;
    }

    /**
     * Writes a 32-bit floating point number and increments position by 4. If necessary, grows the cursor and reallocates the underlying buffer.
     */
    write_f32(value: number): BufferCursor {
        this.ensure_capacity(this.position + 4);

        this.dv.setFloat32(this.position, value, this.little_endian);
        this._position += 4;

        if (this.position > this.size) {
            this.size = this.position;
        }

        return this;
    }

    /**
     * Writes an array of unsigned 8-bit integers and increments position by the array's length. If necessary, grows the cursor and reallocates the underlying buffer.
     */
    write_u8_array(array: number[]): BufferCursor {
        this.ensure_capacity(this.position + array.length);

        new Uint8Array(this.buffer, this.position).set(new Uint8Array(array));
        this._position += array.length;

        if (this.position > this.size) {
            this.size = this.position;
        }

        return this;
    }

    /**
     * Writes the contents of other and increments position by the size of other. If necessary, grows the cursor and reallocates the underlying buffer.
     */
    write_cursor(other: BufferCursor): BufferCursor {
        this.ensure_capacity(this.position + other.size);

        new Uint8Array(this.buffer, this.position).set(new Uint8Array(other.buffer));
        this._position += other.size;

        if (this.position > this.size) {
            this.size = this.position;
        }

        return this;
    }

    write_string_ascii(str: string, byte_length: number): BufferCursor {
        let i = 0;

        for (const byte of ASCII_ENCODER.encode(str)) {
            if (i < byte_length) {
                this.write_u8(byte);
                ++i;
            }
        }

        while (i < byte_length) {
            this.write_u8(0);
            ++i;
        }

        return this;
    }

    /**
     * @returns a Uint8Array that remains a write-through view of the underlying array buffer until the buffer is reallocated.
     */
    uint8_array_view(): Uint8Array {
        return new Uint8Array(this.buffer, 0, this.size);
    }

    private index_of_u8(value: number, max_byte_length: number): number {
        const max_pos = Math.min(this.position + max_byte_length, this.size);

        for (let i = this.position; i < max_pos; ++i) {
            if (this.dv.getUint8(i) === value) {
                return i;
            }
        }

        return this.position + max_byte_length;
    }

    private index_of_u16(value: number, max_byte_length: number): number {
        const max_pos = Math.min(this.position + max_byte_length, this.size);

        for (let i = this.position; i < max_pos; i += 2) {
            if (this.dv.getUint16(i, this.little_endian) === value) {
                return i;
            }
        }

        return this.position + max_byte_length;
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
            new Uint8Array(new_buffer).set(new Uint8Array(this.buffer, 0, this.size));
            this._buffer = new_buffer;
            this.dv = new DataView(this.buffer);
        }
    }
}
