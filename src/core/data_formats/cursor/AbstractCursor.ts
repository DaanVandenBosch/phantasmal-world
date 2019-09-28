import { Endianness } from "../Endianness";
import { Vec2, Vec3 } from "../vector";
import { Cursor } from "./Cursor";

export abstract class AbstractCursor implements Cursor {
    abstract readonly size: number;

    protected _position: number = 0;

    get position(): number {
        return this._position;
    }

    protected little_endian!: boolean;

    get endianness(): Endianness {
        return this.little_endian ? Endianness.Little : Endianness.Big;
    }

    set endianness(endianness: Endianness) {
        this.little_endian = endianness === Endianness.Little;
    }

    get bytes_left(): number {
        return this.size - this.position;
    }

    protected readonly offset: number;

    protected abstract readonly backing_buffer: ArrayBuffer;
    protected abstract readonly dv: DataView;

    constructor(endianness: Endianness, offset: number) {
        this.endianness = endianness;
        this.offset = offset;
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
        this.check_offset(offset, 1);
        return this.dv.getUint8(this.offset + offset);
    }

    u16(): number {
        const r = this.u16_at(this.position);
        this._position += 2;
        return r;
    }

    u16_at(offset: number): number {
        this.check_offset(offset, 2);
        return this.dv.getUint16(this.offset + offset, this.little_endian);
    }

    u32(): number {
        const r = this.u32_at(this.position);
        this._position += 4;
        return r;
    }

    u32_at(offset: number): number {
        this.check_offset(offset, 4);
        return this.dv.getUint32(this.offset + offset, this.little_endian);
    }

    i8(): number {
        return this.i8_at(this._position++);
    }

    i8_at(offset: number): number {
        this.check_offset(offset, 1);
        return this.dv.getInt8(this.offset + offset);
    }

    i16(): number {
        const r = this.i16_at(this.position);
        this._position += 2;
        return r;
    }

    i16_at(offset: number): number {
        this.check_offset(offset, 2);
        return this.dv.getInt16(this.offset + offset, this.little_endian);
    }

    i32(): number {
        const r = this.i32_at(this.position);
        this._position += 4;
        return r;
    }

    i32_at(offset: number): number {
        this.check_offset(offset, 4);
        return this.dv.getInt32(this.offset + offset, this.little_endian);
    }

    f32(): number {
        const r = this.f32_at(this.position);
        this._position += 4;
        return r;
    }

    f32_at(offset: number): number {
        this.check_offset(offset, 4);
        return this.dv.getFloat32(this.offset + offset, this.little_endian);
    }

    u8_array(n: number): number[] {
        this.check_size("n", n, n);

        const array = [];

        for (let i = 0; i < n; ++i) {
            array.push(this.dv.getUint8(this.offset + this._position++));
        }

        return array;
    }

    u16_array(n: number): number[] {
        this.check_size("n", n, 2 * n);

        const array = [];

        for (let i = 0; i < n; ++i) {
            array.push(this.dv.getUint16(this.offset + this.position, this.little_endian));
            this._position += 2;
        }

        return array;
    }

    u32_array(n: number): number[] {
        this.check_size("n", n, 4 * n);

        const array = [];

        for (let i = 0; i < n; ++i) {
            array.push(this.dv.getUint32(this.offset + this.position, this.little_endian));
            this._position += 4;
        }

        return array;
    }

    i32_array(n: number): number[] {
        this.check_size("n", n, 4 * n);

        const array = [];

        for (let i = 0; i < n; ++i) {
            array.push(this.dv.getInt32(this.offset + this.position, this.little_endian));
            this._position += 4;
        }

        return array;
    }

    vec2_f32(): Vec2 {
        return { x: this.f32(), y: this.f32() };
    }

    vec3_f32(): Vec3 {
        return { x: this.f32(), y: this.f32(), z: this.f32() };
    }

    string_ascii(
        max_byte_length: number,
        null_terminated: boolean,
        drop_remaining: boolean,
    ): string {
        let code_points: number[] = [];

        for (let i = 0; i < max_byte_length; i++) {
            const code_point = this.u8();

            if (null_terminated && code_point === 0) {
                if (drop_remaining) {
                    this.seek(max_byte_length - i - 1);
                }

                break;
            }

            code_points.push(code_point);
        }

        return String.fromCodePoint(...code_points);
    }

    string_utf16(
        max_byte_length: number,
        null_terminated: boolean,
        drop_remaining: boolean,
    ): string {
        let code_points: number[] = [];
        let len = Math.floor(max_byte_length / 2);

        for (let i = 0; i < len; i++) {
            const code_point = this.u16();

            if (null_terminated && code_point === 0) {
                if (drop_remaining) {
                    this.seek(2 * (len - i - 1));
                }

                break;
            }

            code_points.push(code_point);
        }

        return String.fromCodePoint(...code_points);
    }

    array_buffer(size: number = this.size - this.position): ArrayBuffer {
        this.check_size("size", size, size);
        const r = this.backing_buffer.slice(
            this.offset + this.position,
            this.offset + this.position + size,
        );
        this._position += size;
        return r;
    }

    copy_to_uint8_array(array: Uint8Array, size: number = this.size - this.position): this {
        this.check_size("size", size, size);
        array.set(new Uint8Array(this.backing_buffer, this.offset + this.position, size));
        this._position += size;
        return this;
    }

    abstract take(size: number): Cursor;

    protected check_size(name: string, value: number, byte_size: number): void {
        if (byte_size < 0 || byte_size > this.size - this.position) {
            throw new Error(`${name} ${value} is out of bounds.`);
        }
    }

    /**
     * Checks whether we can read size bytes at offset.
     */
    private check_offset(offset: number, size: number): void {
        if (offset < 0 || offset + size > this.size) {
            throw new Error(`Offset ${offset} is out of bounds.`);
        }
    }
}
