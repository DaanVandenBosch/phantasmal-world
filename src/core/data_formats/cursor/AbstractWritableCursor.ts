import { Vec2, Vec3 } from "../vector";
import { AbstractCursor } from "./AbstractCursor";
import { Cursor } from "./Cursor";
import { WritableCursor } from "./WritableCursor";

export abstract class AbstractWritableCursor extends AbstractCursor implements WritableCursor {
    abstract size: number;

    write_u8(value: number): this {
        this.write_u8_at(this.position, value);
        this._position += 1;
        return this;
    }

    write_u16(value: number): this {
        this.write_u16_at(this.position, value);
        this._position += 2;
        return this;
    }

    write_u32(value: number): this {
        this.write_u32_at(this.position, value);
        this._position += 4;
        return this;
    }

    write_i8(value: number): this {
        this.write_i8_at(this.position, value);
        this._position += 1;
        return this;
    }

    write_i16(value: number): this {
        this.write_i16_at(this.position, value);
        this._position += 2;
        return this;
    }

    write_i32(value: number): this {
        this.write_i32_at(this.position, value);
        this._position += 4;
        return this;
    }

    write_f32(value: number): this {
        this.write_f32_at(this.position, value);
        this._position += 4;
        return this;
    }

    write_u8_array(array: readonly number[]): this {
        this.write_u8_array_at(this.position, array);
        this._position += array.length;
        return this;
    }

    write_u16_array(array: readonly number[]): this {
        this.write_u16_array_at(this.position, array);
        this._position += array.length * 2;
        return this;
    }

    write_u32_array(array: readonly number[]): this {
        this.write_u32_array_at(this.position, array);
        this._position += array.length * 4;
        return this;
    }

    write_vec2_f32(value: Vec2): this {
        this.write_vec2_f32_at(this.position, value);
        this._position += 8;
        return this;
    }

    write_vec3_f32(value: Vec3): this {
        this.write_vec3_f32_at(this.position, value);
        this._position += 12;
        return this;
    }

    write_cursor(other: Cursor): this {
        const size = other.size - other.position;
        this.ensure_size(size);

        other.copy_to_uint8_array(
            new Uint8Array(this.backing_buffer, this.offset + this.position, size),
            size,
        );

        this._position += size;
        return this;
    }

    write_string_ascii(str: string, byte_length: number): this {
        this.write_string_ascii_at(this.position, str, byte_length);
        this._position += byte_length;
        return this;
    }

    write_string_utf16(str: string, byte_length: number): this {
        this.write_string_utf16_at(this.position, str, byte_length);
        this._position += byte_length;
        return this;
    }

    write_u8_at(offset: number, value: number): this {
        this.ensure_size(1, offset);
        this.dv.setUint8(offset, value);
        return this;
    }

    write_u16_at(offset: number, value: number): this {
        this.ensure_size(2, offset);
        this.dv.setUint16(offset, value, this.little_endian);
        return this;
    }

    write_u32_at(offset: number, value: number): this {
        this.ensure_size(4, offset);
        this.dv.setUint32(offset, value, this.little_endian);
        return this;
    }

    write_i8_at(offset: number, value: number): this {
        this.ensure_size(1, offset);
        this.dv.setInt8(offset, value);
        return this;
    }

    write_i16_at(offset: number, value: number): this {
        this.ensure_size(2, offset);
        this.dv.setInt16(offset, value, this.little_endian);
        return this;
    }

    write_i32_at(offset: number, value: number): this {
        this.ensure_size(4, offset);
        this.dv.setInt32(offset, value, this.little_endian);
        return this;
    }

    write_f32_at(offset: number, value: number): this {
        this.ensure_size(4, offset);
        this.dv.setFloat32(offset, value, this.little_endian);
        return this;
    }

    write_u8_array_at(offset: number, array: readonly number[]): this {
        this.ensure_size(array.length, offset);
        new Uint8Array(this.backing_buffer, this.offset + offset).set(new Uint8Array(array));
        return this;
    }

    write_u16_array_at(offset: number, array: readonly number[]): this {
        this.ensure_size(2 * array.length, offset);
        const len = array.length;

        for (let i = 0; i < len; i++) {
            this.write_u16_at(offset + i * 2, array[i]);
        }

        return this;
    }

    write_u32_array_at(offset: number, array: readonly number[]): this {
        this.ensure_size(4 * array.length, offset);
        const len = array.length;

        for (let i = 0; i < len; i++) {
            this.write_u32_at(offset + i * 4, array[i]);
        }

        return this;
    }

    write_vec2_f32_at(offset: number, value: Vec2): this {
        this.ensure_size(8, offset);
        this.dv.setFloat32(offset, value.x, this.little_endian);
        this.dv.setFloat32(offset + 4, value.y, this.little_endian);
        return this;
    }

    write_vec3_f32_at(offset: number, value: Vec3): this {
        this.ensure_size(12, offset);
        this.dv.setFloat32(offset, value.x, this.little_endian);
        this.dv.setFloat32(offset + 4, value.y, this.little_endian);
        this.dv.setFloat32(offset + 8, value.z, this.little_endian);
        return this;
    }

    write_string_ascii_at(offset: number, str: string, byte_length: number): this {
        this.ensure_size(byte_length, offset);

        const len = Math.min(byte_length, str.length);

        for (let i = 0; i < len; i++) {
            this.write_u8_at(offset + i, str.codePointAt(i)!);
        }

        const pad_len = byte_length - len;

        for (let i = 0; i < pad_len; i++) {
            this.write_u8_at(offset + len + i, 0);
        }

        return this;
    }

    write_string_utf16_at(offset: number, str: string, byte_length: number): this {
        this.ensure_size(byte_length, offset);

        const max_len = Math.floor(byte_length / 2);
        const len = Math.min(max_len, str.length);

        for (let i = 0; i < len; i++) {
            this.write_u16_at(offset + i * 2, str.codePointAt(i)!);
        }

        const pad_len = max_len - len;

        for (let i = 0; i < pad_len; i++) {
            this.write_u16_at(offset + len * 2 + i * 2, 0);
        }

        return this;
    }

    protected ensure_size(size: number, offset: number = this.position): void {
        const left = this.size - offset;
        if (size > left) {
            throw new Error(`${size} Bytes required but only ${left} available.`);
        }
    }
}
