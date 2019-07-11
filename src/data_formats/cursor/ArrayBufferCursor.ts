import {
    ASCII_DECODER,
    UTF_16BE_DECODER,
    UTF_16BE_ENCODER,
    UTF_16LE_DECODER,
    UTF_16LE_ENCODER,
} from ".";
import { Endianness } from "..";
import { Cursor } from "./Cursor";
import { Vec3 } from "../Vec3";

/**
 * A cursor for reading from an array buffer or part of an array buffer.
 */
export class ArrayBufferCursor implements Cursor {
    get offset(): number {
        return this.dv.byteOffset;
    }

    get size(): number {
        return this.dv.byteLength;
    }

    set size(size: number) {
        this.dv = new DataView(this.buffer, this.offset, size);
    }

    protected _position: number;

    get position(): number {
        return this._position;
    }

    protected little_endian!: boolean;

    get endianness(): Endianness {
        return this.little_endian ? Endianness.Little : Endianness.Big;
    }

    set endianness(endianness: Endianness) {
        this.little_endian = endianness === Endianness.Little;
        this.utf16_decoder = this.little_endian ? UTF_16LE_DECODER : UTF_16BE_DECODER;
        this.utf16_encoder = this.little_endian ? UTF_16LE_ENCODER : UTF_16BE_ENCODER;
    }

    get bytes_left(): number {
        return this.size - this.position;
    }

    protected buffer: ArrayBuffer;
    protected dv: DataView;

    private utf16_decoder: TextDecoder = UTF_16BE_DECODER;
    private utf16_encoder: TextEncoder = UTF_16BE_ENCODER;

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
        size: number = buffer.byteLength
    ) {
        this.buffer = buffer;
        this.dv = new DataView(buffer, offset, size);
        this.endianness = endianness;
        this._position = 0;
    }

    seek(offset: number): this {
        return this.seek_start(this.position + offset);
    }

    seek_start(offset: number): this {
        if (offset < 0 || offset > this.size) {
            throw new Error(`Offset ${offset} is out of bounds.`);
        }

        this._position = offset;
        return this;
    }

    seek_end(offset: number): this {
        if (offset < 0 || offset > this.size) {
            throw new Error(`Offset ${offset} is out of bounds.`);
        }

        this._position = this.size - offset;
        return this;
    }

    u8(): number {
        return this.u8_at(this._position++);
    }

    u8_at(offset: number): number {
        return this.dv.getUint8(offset);
    }

    u16(): number {
        const r = this.u16_at(this.position);
        this._position += 2;
        return r;
    }

    u16_at(offset: number): number {
        return this.dv.getUint16(offset, this.little_endian);
    }

    u32(): number {
        const r = this.u32_at(this.position);
        this._position += 4;
        return r;
    }

    u32_at(offset: number): number {
        return this.dv.getUint32(offset, this.little_endian);
    }

    i8(): number {
        return this.i8_at(this._position++);
    }

    i8_at(offset: number): number {
        return this.dv.getInt8(offset);
    }

    i16(): number {
        const r = this.i16_at(this.position);
        this._position += 2;
        return r;
    }

    i16_at(offset: number): number {
        return this.dv.getInt16(offset, this.little_endian);
    }

    i32(): number {
        const r = this.i32_at(this.position);
        this._position += 4;
        return r;
    }

    i32_at(offset: number): number {
        return this.dv.getInt32(offset, this.little_endian);
    }

    f32(): number {
        const r = this.f32_at(this.position);
        this._position += 4;
        return r;
    }

    f32_at(offset: number): number {
        return this.dv.getFloat32(offset, this.little_endian);
    }

    u8_array(n: number): number[] {
        const array = [];
        for (let i = 0; i < n; ++i) array.push(this.dv.getUint8(this._position++));
        return array;
    }

    u16_array(n: number): number[] {
        const array = [];

        for (let i = 0; i < n; ++i) {
            array.push(this.dv.getUint16(this.position, this.little_endian));
            this._position += 2;
        }

        return array;
    }

    u32_array(n: number): number[] {
        const array = [];

        for (let i = 0; i < n; ++i) {
            array.push(this.dv.getUint32(this.position, this.little_endian));
            this._position += 4;
        }

        return array;
    }

    vec3(): Vec3 {
        return new Vec3(this.f32(), this.f32(), this.f32());
    }

    take(size: number): ArrayBufferCursor {
        const offset = this.offset + this.position;
        const wrapper = new ArrayBufferCursor(this.buffer, this.endianness, offset, size);
        this._position += size;
        return wrapper;
    }

    string_ascii(
        max_byte_length: number,
        null_terminated: boolean,
        drop_remaining: boolean
    ): string {
        const string_length = null_terminated
            ? this.index_of_u8(0, max_byte_length) - this.position
            : max_byte_length;

        const view = new DataView(this.buffer, this.offset + this.position, string_length);
        const r = ASCII_DECODER.decode(view);

        this._position += drop_remaining
            ? max_byte_length
            : Math.min(string_length + 1, max_byte_length);

        return r;
    }

    string_utf16(
        max_byte_length: number,
        null_terminated: boolean,
        drop_remaining: boolean
    ): string {
        const string_length = null_terminated
            ? this.index_of_u16(0, max_byte_length) - this.position
            : Math.floor(max_byte_length / 2) * 2;

        const view = new DataView(this.buffer, this.offset + this.position, string_length);
        const r = this.utf16_decoder.decode(view);

        this._position += drop_remaining
            ? max_byte_length
            : Math.min(string_length + 2, max_byte_length);

        return r;
    }

    array_buffer(size: number = this.size - this.position): ArrayBuffer {
        const r = this.buffer.slice(
            this.offset + this.position,
            this.offset + this.position + size
        );
        this._position += size;
        return r;
    }

    copy_to_uint8_array(array: Uint8Array, size: number = this.size - this.position): this {
        array.set(new Uint8Array(this.buffer, this.offset + this.position, size));
        this._position += size;
        return this;
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
}
