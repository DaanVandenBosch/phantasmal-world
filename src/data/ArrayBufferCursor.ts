// TODO: remove dependency on text-encoding because it is no longer maintained.
import { TextDecoder, TextEncoder } from 'text-encoding';

const ASCII_DECODER = new TextDecoder('ascii');
const UTF_16BE_DECODER = new TextDecoder('utf-16be');
const UTF_16LE_DECODER = new TextDecoder('utf-16le');

const ASCII_ENCODER = new TextEncoder('ascii');
const UTF_16BE_ENCODER = new TextEncoder('utf-16be');
const UTF_16LE_ENCODER = new TextEncoder('utf-16le');

/**
 * A cursor for reading and writing binary data.
 * Uses an ArrayBuffer internally. This buffer is reallocated if and only if a write beyond the current capacity happens.
 */
export class ArrayBufferCursor {
    private _size: number = 0;

    /**
     * The cursor's size. This value will always be non-negative and equal to or smaller than the cursor's capacity.
     */
    get size(): number {
        return this._size;
    }

    set size(size: number) {
        if (size < 0) {
            throw new Error('Size should be non-negative.')
        }

        this.ensureCapacity(size);
        this._size = size;
    }

    /**
     * The position from where bytes will be read or written.
     */
    position: number;

    /**
     * Byte order mode.
     */
    littleEndian: boolean;

    /**
     * The amount of bytes left to read from the current position onward.
     */
    get bytesLeft(): number {
        return this.size - this.position;
    }

    /**
     * The size of the underlying buffer. This value will always be equal to or greater than the cursor's size.
     */
    get capacity(): number {
        return this.buffer.byteLength;
    }

    buffer: ArrayBuffer;

    private dv: DataView;
    private uint8Array: Uint8Array;
    private utf16Decoder: TextDecoder;
    private utf16Encoder: TextEncoder;

    /**
     * @param bufferOrCapacity - If an ArrayBuffer is given, writes to the cursor will be reflected in this array buffer and vice versa until a cursor write that requires allocating a new internal buffer happens
     * @param littleEndian - Decides in which byte order multi-byte integers and floats will be interpreted
     */
    constructor(bufferOrCapacity: ArrayBuffer | number, littleEndian?: boolean) {
        if (typeof bufferOrCapacity === 'number') {
            this.buffer = new ArrayBuffer(bufferOrCapacity);
            this.size = 0;
        } else if (bufferOrCapacity instanceof ArrayBuffer) {
            this.buffer = bufferOrCapacity;
            this.size = this.buffer.byteLength;
        } else {
            throw new Error('buffer_or_capacity should be an ArrayBuffer or a number.');
        }

        this.littleEndian = !!littleEndian;
        this.position = 0;
        this.dv = new DataView(this.buffer);
        this.uint8Array = new Uint8Array(this.buffer, 0, this.size);
        this.utf16Decoder = littleEndian ? UTF_16LE_DECODER : UTF_16BE_DECODER;
        this.utf16Encoder = littleEndian ? UTF_16LE_ENCODER : UTF_16BE_ENCODER;
    }

    /**
     * Seek forward or backward by a number of bytes.
     * 
     * @param offset - if positive, seeks forward by offset bytes, otherwise seeks backward by -offset bytes.
     */
    seek(offset: number) {
        return this.seekStart(this.position + offset);
    }

    /**
     * Seek forward from the start of the cursor by a number of bytes.
     * 
     * @param offset - greater or equal to 0 and smaller than size
     */
    seekStart(offset: number) {
        if (offset < 0 || offset > this.size) {
            throw new Error(`Offset ${offset} is out of bounds.`);
        }

        this.position = offset;
        return this;
    }

    /**
     * Seek backward from the end of the cursor by a number of bytes.
     * 
     * @param offset - greater or equal to 0 and smaller than size
     */
    seekEnd(offset: number) {
        if (offset < 0 || offset > this.size) {
            throw new Error(`Offset ${offset} is out of bounds.`);
        }

        this.position = this.size - offset;
        return this;
    }

    /**
     * Reads an unsigned 8-bit integer and increments position by 1.
     */
    u8() {
        return this.dv.getUint8(this.position++);
    }

    /**
     * Reads an unsigned 16-bit integer and increments position by 2.
     */
    u16() {
        const r = this.dv.getUint16(this.position, this.littleEndian);
        this.position += 2;
        return r;
    }

    /**
     * Reads an unsigned 32-bit integer and increments position by 4.
     */
    u32() {
        const r = this.dv.getUint32(this.position, this.littleEndian);
        this.position += 4;
        return r;
    }

    /**
     * Reads a signed 16-bit integer and increments position by 2.
     */
    i16() {
        const r = this.dv.getInt16(this.position, this.littleEndian);
        this.position += 2;
        return r;
    }

    /**
     * Reads a signed 32-bit integer and increments position by 4.
     */
    i32() {
        const r = this.dv.getInt32(this.position, this.littleEndian);
        this.position += 4;
        return r;
    }

    /**
     * Reads a 32-bit floating point number and increments position by 4.
     */
    f32() {
        const r = this.dv.getFloat32(this.position, this.littleEndian);
        this.position += 4;
        return r;
    }

    /**
     * Reads n unsigned 8-bit integers and increments position by n.
     */
    u8Array(n: number): number[] {
        const array = [];
        for (let i = 0; i < n; ++i) array.push(this.dv.getUint8(this.position++));
        return array;
    }

    /**
     * Reads n unsigned 16-bit integers and increments position by 2n.
     */
    u16Array(n: number): number[] {
        const array = [];

        for (let i = 0; i < n; ++i) {
            array.push(this.dv.getUint16(this.position, this.littleEndian));
            this.position += 2;
        }

        return array;
    }

    /**
     * Consumes a variable number of bytes.
     * 
     * @param size - the amount bytes to consume.
     * @returns a new cursor containing size bytes.
     */
    take(size: number): ArrayBufferCursor {
        if (size < 0 || size > this.size - this.position) {
            throw new Error(`Size ${size} out of bounds.`);
        }

        this.position += size;
        return new ArrayBufferCursor(
            this.buffer.slice(this.position - size, this.position), this.littleEndian);
    }

    /**
     * Consumes up to maxByteLength bytes.
     */
    stringAscii(maxByteLength: number, nullTerminated: boolean, dropRemaining: boolean) {
        const string_length = nullTerminated
            ? this.indexOfU8(0, maxByteLength) - this.position
            : maxByteLength;

        const r = ASCII_DECODER.decode(
            new DataView(this.buffer, this.position, string_length));
        this.position += dropRemaining
            ? maxByteLength
            : Math.min(string_length + 1, maxByteLength);
        return r;
    }

    /**
     * Consumes up to maxByteLength bytes.
     */
    stringUtf16(maxByteLength: number, nullTerminated: boolean, dropRemaining: boolean) {
        const stringLength = nullTerminated
            ? this.indexOfU16(0, maxByteLength) - this.position
            : Math.floor(maxByteLength / 2) * 2;

        const r = this.utf16Decoder.decode(
            new DataView(this.buffer, this.position, stringLength));
        this.position += dropRemaining
            ? maxByteLength
            : Math.min(stringLength + 2, maxByteLength);
        return r;
    }

    /**
     * Writes an unsigned 8-bit integer and increments position by 1. If necessary, grows the cursor and reallocates the underlying buffer.
     */
    writeU8(value: number) {
        this.ensureCapacity(this.position + 1);

        this.dv.setUint8(this.position++, value);

        if (this.position > this.size) {
            this.size = this.position;
        }

        return this;
    }

    /**
     * Writes an unsigned 16-bit integer and increments position by 2. If necessary, grows the cursor and reallocates the underlying buffer.
     */
    writeU16(value: number) {
        this.ensureCapacity(this.position + 2);

        this.dv.setUint16(this.position, value, this.littleEndian);
        this.position += 2;

        if (this.position > this.size) {
            this.size = this.position;
        }

        return this;
    }

    /**
     * Writes an unsigned 32-bit integer and increments position by 4. If necessary, grows the cursor and reallocates the underlying buffer.
     */
    writeU32(value: number) {
        this.ensureCapacity(this.position + 4);

        this.dv.setUint32(this.position, value, this.littleEndian);
        this.position += 4;

        if (this.position > this.size) {
            this.size = this.position;
        }

        return this;
    }

    /**
     * Writes a signed 32-bit integer and increments position by 4. If necessary, grows the cursor and reallocates the underlying buffer.
     */
    writeI32(value: number) {
        this.ensureCapacity(this.position + 4);

        this.dv.setInt32(this.position, value, this.littleEndian);
        this.position += 4;

        if (this.position > this.size) {
            this.size = this.position;
        }

        return this;
    }

    /**
     * Writes a 32-bit floating point number and increments position by 4. If necessary, grows the cursor and reallocates the underlying buffer.
     */
    writeF32(value: number) {
        this.ensureCapacity(this.position + 4);

        this.dv.setFloat32(this.position, value, this.littleEndian);
        this.position += 4;

        if (this.position > this.size) {
            this.size = this.position;
        }

        return this;
    }

    /**
     * Writes an array of unsigned 8-bit integers and increments position by the array's length. If necessary, grows the cursor and reallocates the underlying buffer.
     */
    writeU8Array(array: number[]) {
        this.ensureCapacity(this.position + array.length);

        new Uint8Array(this.buffer, this.position).set(new Uint8Array(array));
        this.position += array.length;

        if (this.position > this.size) {
            this.size = this.position;
        }

        return this;
    }

    /**
     * Writes the contents of other and increments position by the size of other. If necessary, grows the cursor and reallocates the underlying buffer.
     */
    writeCursor(other: ArrayBufferCursor) {
        this.ensureCapacity(this.position + other.size);

        new Uint8Array(this.buffer, this.position).set(new Uint8Array(other.buffer));
        this.position += other.size;

        if (this.position > this.size) {
            this.size = this.position;
        }

        return this;
    }

    writeStringAscii(str: string, byteLength: number) {
        let i = 0;

        for (const byte of ASCII_ENCODER.encode(str)) {
            if (i < byteLength) {
                this.writeU8(byte);
                ++i;
            }
        }

        while (i < byteLength) {
            this.writeU8(0);
            ++i;
        }
    }

    /**
     * @returns a Uint8Array that remains a write-through view of the underlying array buffer until the buffer is reallocated.
     */
    uint8ArrayView(): Uint8Array {
        return this.uint8Array;
    }

    private indexOfU8(value: number, maxByteLength: number) {
        const maxPos = Math.min(this.position + maxByteLength, this.size);

        for (let i = this.position; i < maxPos; ++i) {
            if (this.dv.getUint8(i) === value) {
                return i;
            }
        }

        return this.position + maxByteLength;
    }

    private indexOfU16(value: number, maxByteLength: number) {
        const maxPos = Math.min(this.position + maxByteLength, this.size);

        for (let i = this.position; i < maxPos; i += 2) {
            if (this.dv.getUint16(i, this.littleEndian) === value) {
                return i;
            }
        }

        return this.position + maxByteLength;
    }

    /**
     *  Increases buffer size if necessary.
     */
    private ensureCapacity(minNewSize: number) {
        if (minNewSize > this.capacity) {
            let newSize = this.capacity || minNewSize;

            do {
                newSize *= 2;
            } while (newSize < minNewSize);

            const newBuffer = new ArrayBuffer(newSize);
            new Uint8Array(newBuffer).set(new Uint8Array(this.buffer, 0, this.size));
            this.buffer = newBuffer;
            this.dv = new DataView(this.buffer);
            this.uint8Array = new Uint8Array(this.buffer, 0, minNewSize);
        }
    }
}
