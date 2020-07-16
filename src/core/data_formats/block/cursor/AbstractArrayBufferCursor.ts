import { Endianness } from "../Endianness";
import { Cursor } from "./Cursor";
import { AbstractWritableCursor } from "./AbstractWritableCursor";

export abstract class AbstractArrayBufferCursor extends AbstractWritableCursor {
    protected little_endian!: boolean;

    get endianness(): Endianness {
        return this.little_endian ? Endianness.Little : Endianness.Big;
    }

    set endianness(endianness: Endianness) {
        this.little_endian = endianness === Endianness.Little;
    }

    protected abstract readonly backing_buffer: ArrayBuffer;
    protected abstract readonly dv: DataView;

    protected constructor(endianness: Endianness, offset: number) {
        super(offset);

        this.endianness = endianness;
    }

    u8(): number {
        this.check_size(1);
        const r = this.dv.getUint8(this.absolute_position);
        this._position++;
        return r;
    }

    u16(): number {
        this.check_size(2);
        const r = this.dv.getUint16(this.absolute_position, this.little_endian);
        this._position += 2;
        return r;
    }

    u32(): number {
        this.check_size(4);
        const r = this.dv.getUint32(this.absolute_position, this.little_endian);
        this._position += 4;
        return r;
    }

    i8(): number {
        this.check_size(1);
        const r = this.dv.getInt8(this.absolute_position);
        this._position++;
        return r;
    }

    i16(): number {
        this.check_size(2);
        const r = this.dv.getInt16(this.absolute_position, this.little_endian);
        this._position += 2;
        return r;
    }

    i32(): number {
        this.check_size(4);
        const r = this.dv.getInt32(this.absolute_position, this.little_endian);
        this._position += 4;
        return r;
    }

    f32(): number {
        this.check_size(4);
        const r = this.dv.getFloat32(this.absolute_position, this.little_endian);
        this._position += 4;
        return r;
    }

    u8_array(n: number): number[] {
        this.check_size(n);

        const array = [];

        for (let i = 0; i < n; ++i) {
            array.push(this.dv.getUint8(this.absolute_position));
            this._position++;
        }

        return array;
    }

    u16_array(n: number): number[] {
        this.check_size(2 * n);

        const array = [];

        for (let i = 0; i < n; ++i) {
            array.push(this.dv.getUint16(this.absolute_position, this.little_endian));
            this._position += 2;
        }

        return array;
    }

    u32_array(n: number): number[] {
        this.check_size(4 * n);

        const array = [];

        for (let i = 0; i < n; ++i) {
            array.push(this.dv.getUint32(this.absolute_position, this.little_endian));
            this._position += 4;
        }

        return array;
    }

    i32_array(n: number): number[] {
        this.check_size(4 * n);

        const array = [];

        for (let i = 0; i < n; ++i) {
            array.push(this.dv.getInt32(this.absolute_position, this.little_endian));
            this._position += 4;
        }

        return array;
    }

    abstract take(size: number): Cursor;

    array_buffer(size: number = this.size - this.position): ArrayBuffer {
        this.check_size(size);
        const r = this.backing_buffer.slice(this.absolute_position, this.absolute_position + size);
        this._position += size;
        return r;
    }

    copy_to_uint8_array(array: Uint8Array, size: number = this.size - this.position): this {
        this.check_size(size);
        array.set(new Uint8Array(this.backing_buffer, this.absolute_position, size));
        this._position += size;
        return this;
    }

    write_u8(value: number): this {
        this.check_size(1);
        this.dv.setUint8(this.absolute_position, value);
        this._position++;
        return this;
    }

    write_u16(value: number): this {
        this.check_size(2);
        this.dv.setUint16(this.absolute_position, value, this.little_endian);
        this._position += 2;
        return this;
    }

    write_u32(value: number): this {
        this.check_size(4);
        this.dv.setUint32(this.absolute_position, value, this.little_endian);
        this._position += 4;
        return this;
    }

    write_i8(value: number): this {
        this.check_size(1);
        this.dv.setInt8(this.absolute_position, value);
        this._position++;
        return this;
    }

    write_i16(value: number): this {
        this.check_size(2);
        this.dv.setInt16(this.absolute_position, value, this.little_endian);
        this._position += 2;
        return this;
    }

    write_i32(value: number): this {
        this.check_size(4);
        this.dv.setInt32(this.absolute_position, value, this.little_endian);
        this._position += 4;
        return this;
    }

    write_f32(value: number): this {
        this.check_size(4);
        this.dv.setFloat32(this.absolute_position, value, this.little_endian);
        this._position += 4;
        return this;
    }

    write_u8_array(array: ArrayLike<number>): this {
        this.check_size(array.length);
        new Uint8Array(this.backing_buffer, this.absolute_position, array.length).set(
            new Uint8Array(array),
        );
        this._position += array.length;
        return this;
    }

    write_cursor(other: Cursor): this {
        const size = other.size - other.position;
        this.check_size(size);

        other.copy_to_uint8_array(
            new Uint8Array(this.backing_buffer, this.absolute_position, size),
            size,
        );

        this._position += size;
        return this;
    }
}
