import { WritableBlock } from "./WritableBlock";
import { Endianness } from "./Endianness";

export abstract class AbstractWritableBlock implements WritableBlock {
    abstract readonly size: number;

    protected little_endian!: boolean;

    get endianness(): Endianness {
        return this.little_endian ? Endianness.Little : Endianness.Big;
    }

    set endianness(endianness: Endianness) {
        this.little_endian = endianness === Endianness.Little;
    }

    protected abstract readonly buffer: ArrayBuffer;
    protected abstract readonly data_view: DataView;

    protected constructor(endianness: Endianness) {
        this.endianness = endianness;
    }

    get_u8(offset: number): number {
        this.check_offset(offset, 1);
        return this.data_view.getUint8(offset);
    }

    get_u16(offset: number): number {
        this.check_offset(offset, 2);
        return this.data_view.getUint16(offset, this.little_endian);
    }

    get_u32(offset: number): number {
        this.check_offset(offset, 4);
        return this.data_view.getUint32(offset, this.little_endian);
    }

    get_i8(offset: number): number {
        this.check_offset(offset, 1);
        return this.data_view.getInt8(offset);
    }

    get_i16(offset: number): number {
        this.check_offset(offset, 2);
        return this.data_view.getInt16(offset, this.little_endian);
    }

    get_i32(offset: number): number {
        this.check_offset(offset, 4);
        return this.data_view.getInt32(offset, this.little_endian);
    }

    get_f32(offset: number): number {
        this.check_offset(offset, 4);
        return this.data_view.getFloat32(offset, this.little_endian);
    }

    get_string_utf16(offset: number, max_byte_length: number, null_terminated: boolean): string {
        const code_points: number[] = [];
        const len = Math.floor(max_byte_length / 2);

        for (let i = 0; i < len; i++) {
            const code_point = this.get_u16(offset + i * 2);

            if (null_terminated && code_point === 0) {
                break;
            }

            code_points.push(code_point);
        }

        return String.fromCodePoint(...code_points);
    }

    get_array_buffer(offset: number, size: number): ArrayBuffer {
        this.check_offset(offset, size);
        return this.buffer.slice(offset, offset + size);
    }

    uint8_view(offset: number, size: number): Uint8Array {
        this.check_offset(offset, size);
        return new Uint8Array(this.buffer, offset, size);
    }

    set_u8(offset: number, value: number): this {
        this.check_offset(offset, 1);
        this.data_view.setUint8(offset, value);
        return this;
    }

    set_u16(offset: number, value: number): this {
        this.check_offset(offset, 2);
        this.data_view.setUint16(offset, value, this.little_endian);
        return this;
    }

    set_u32(offset: number, value: number): this {
        this.check_offset(offset, 4);
        this.data_view.setUint32(offset, value, this.little_endian);
        return this;
    }

    set_i8(offset: number, value: number): this {
        this.check_offset(offset, 1);
        this.data_view.setInt8(offset, value);
        return this;
    }

    set_i16(offset: number, value: number): this {
        this.check_offset(offset, 2);
        this.data_view.setInt16(offset, value, this.little_endian);
        return this;
    }

    set_i32(offset: number, value: number): this {
        this.check_offset(offset, 4);
        this.data_view.setInt32(offset, value, this.little_endian);
        return this;
    }

    set_f32(offset: number, value: number): this {
        this.check_offset(offset, 4);
        this.data_view.setFloat32(offset, value, this.little_endian);
        return this;
    }

    zero(): this {
        new Uint8Array(this.buffer).fill(0);
        return this;
    }

    /**
     * Checks whether we can read `size` bytes at `offset`.
     */
    protected check_offset(offset: number, size: number): void {
        if (offset < 0 || offset + size > this.size) {
            throw new Error(`Offset ${offset} is out of bounds.`);
        }
    }
}
