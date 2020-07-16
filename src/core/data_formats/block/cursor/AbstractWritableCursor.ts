import { WritableCursor } from "./WritableCursor";
import { Endianness } from "../Endianness";
import { Vec2, Vec3 } from "../../vector";
import { Cursor } from "./Cursor";

export abstract class AbstractWritableCursor implements WritableCursor {
    abstract size: number;

    protected _position: number = 0;

    get position(): number {
        return this._position;
    }

    abstract endianness: Endianness;

    get bytes_left(): number {
        return this.size - this._position;
    }

    protected readonly offset: number;

    protected get absolute_position(): number {
        return this.offset + this._position;
    }

    protected constructor(offset: number) {
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

    abstract u8(): number;

    abstract u16(): number;

    abstract u32(): number;

    abstract i8(): number;

    abstract i16(): number;

    abstract i32(): number;

    abstract f32(): number;

    abstract u8_array(n: number): number[];

    abstract u16_array(n: number): number[];

    abstract u32_array(n: number): number[];

    abstract i32_array(n: number): number[];

    vec2_f32(): Vec2 {
        return { x: this.f32(), y: this.f32() };
    }

    vec3_f32(): Vec3 {
        return { x: this.f32(), y: this.f32(), z: this.f32() };
    }

    abstract take(size: number): Cursor;

    string_ascii(
        max_byte_length: number,
        null_terminated: boolean,
        drop_remaining: boolean,
    ): string {
        const code_points: number[] = [];

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
        const code_points: number[] = [];
        const len = Math.floor(max_byte_length / 2);

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

    abstract array_buffer(size?: number): ArrayBuffer;

    abstract copy_to_uint8_array(array: Uint8Array, size?: number): this;

    abstract write_u8(value: number): this;

    abstract write_u16(value: number): this;

    abstract write_u32(value: number): this;

    abstract write_i8(value: number): this;

    abstract write_i16(value: number): this;

    abstract write_i32(value: number): this;

    abstract write_f32(value: number): this;

    write_u8_array(array: ArrayLike<number>): this {
        const len = array.length;
        this.check_size(len);

        for (let i = 0; i < len; i++) {
            this.write_u8(array[i]);
        }

        return this;
    }

    write_u16_array(array: ArrayLike<number>): this {
        const len = array.length;
        this.check_size(2 * len);

        for (let i = 0; i < len; i++) {
            this.write_u16(array[i]);
        }

        return this;
    }

    write_u32_array(array: ArrayLike<number>): this {
        const len = array.length;
        this.check_size(4 * len);

        for (let i = 0; i < len; i++) {
            this.write_u32(array[i]);
        }

        return this;
    }

    write_i32_array(array: ArrayLike<number>): this {
        const len = array.length;
        this.check_size(4 * len);

        for (let i = 0; i < len; i++) {
            this.write_i32(array[i]);
        }

        return this;
    }

    write_vec2_f32(value: Vec2): this {
        this.check_size(8);
        this.write_f32(value.x);
        this.write_f32(value.y);
        return this;
    }

    write_vec3_f32(value: Vec3): this {
        this.check_size(12);
        this.write_f32(value.x);
        this.write_f32(value.y);
        this.write_f32(value.z);
        return this;
    }

    abstract write_cursor(other: Cursor): this;

    write_string_ascii(str: string, byte_length: number): this {
        this.check_size(byte_length);

        const len = Math.min(byte_length, str.length);

        for (let i = 0; i < len; i++) {
            this.write_u8(str.codePointAt(i)!);
        }

        const pad_len = byte_length - len;

        for (let i = 0; i < pad_len; i++) {
            this.write_u8(0);
        }

        return this;
    }

    write_string_utf16(str: string, byte_length: number): this {
        this.check_size(byte_length);

        const max_len = Math.floor(byte_length / 2);
        const len = Math.min(max_len, str.length);

        for (let i = 0; i < len; i++) {
            this.write_u16(str.codePointAt(i)!);
        }

        const pad_len = max_len - len;

        for (let i = 0; i < pad_len; i++) {
            this.write_u16(0);
        }

        return this;
    }

    /**
     * Throws an error if less than `size` bytes are left at `position`.
     */
    protected check_size(size: number): void {
        const left = this.size - this._position;

        if (size > left) {
            throw new Error(`${size} Bytes required but only ${left} available.`);
        }
    }
}
