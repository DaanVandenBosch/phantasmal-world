import { Vec2, Vec3 } from "../vector";
import { AbstractCursor } from "./AbstractCursor";
import { Cursor } from "./Cursor";
import { WritableCursor } from "./WritableCursor";

export abstract class AbstractWritableCursor extends AbstractCursor implements WritableCursor {
    abstract size: number;

    write_u8(value: number): this {
        this.ensure_size(1);
        this.dv.setUint8(this._position++, value);
        return this;
    }

    write_u16(value: number): this {
        this.ensure_size(2);
        this.dv.setUint16(this.position, value, this.little_endian);
        this._position += 2;
        return this;
    }

    write_u32(value: number): this {
        this.ensure_size(4);
        this.dv.setUint32(this.position, value, this.little_endian);
        this._position += 4;
        return this;
    }

    write_i32(value: number): this {
        this.ensure_size(4);
        this.dv.setInt32(this.position, value, this.little_endian);
        this._position += 4;
        return this;
    }

    write_f32(value: number): this {
        this.ensure_size(4);
        this.dv.setFloat32(this.position, value, this.little_endian);
        this._position += 4;
        return this;
    }

    write_u8_array(array: number[]): this {
        this.ensure_size(array.length);
        new Uint8Array(this.backing_buffer, this.offset + this.position).set(new Uint8Array(array));
        this._position += array.length;
        return this;
    }

    write_u16_array(array: number[]): this {
        this.ensure_size(2 * array.length);
        const len = array.length;

        for (let i = 0; i < len; i++) {
            this.write_u16(array[i]);
        }

        return this;
    }

    write_u32_array(array: number[]): this {
        this.ensure_size(4 * array.length);
        const len = array.length;

        for (let i = 0; i < len; i++) {
            this.write_u32(array[i]);
        }

        return this;
    }

    write_vec2_f32(value: Vec2): this {
        this.ensure_size(8);
        this.dv.setFloat32(this.position, value.x, this.little_endian);
        this.dv.setFloat32(this.position + 4, value.y, this.little_endian);
        this._position += 8;
        return this;
    }

    write_vec3_f32(value: Vec3): this {
        this.ensure_size(12);
        this.dv.setFloat32(this.position, value.x, this.little_endian);
        this.dv.setFloat32(this.position + 4, value.y, this.little_endian);
        this.dv.setFloat32(this.position + 8, value.z, this.little_endian);
        this._position += 12;
        return this;
    }

    write_cursor(other: Cursor): this {
        const size = other.size - other.position;
        this.ensure_size(size);

        other.copy_to_uint8_array(
            new Uint8Array(this.backing_buffer, this.offset + this.position, size),
            size
        );

        this._position += size;
        return this;
    }

    write_string_ascii(str: string, byte_length: number): this {
        this.ensure_size(byte_length);

        const len = Math.min(byte_length, str.length);

        for (let i = 0; i < len; i++) {
            this.write_u8(str.codePointAt(i)!);
        }

        let pad_len = byte_length - len;

        for (let i = 0; i < pad_len; i++) {
            this.write_u8(0);
        }

        return this;
    }

    write_string_utf16(str: string, byte_length: number): this {
        this.ensure_size(byte_length);

        const max_len = Math.floor(byte_length / 2);
        const len = Math.min(max_len, str.length);

        for (let i = 0; i < len; i++) {
            this.write_u16(str.codePointAt(i)!);
        }

        let pad_len = max_len - len;

        for (let i = 0; i < pad_len; i++) {
            this.write_u16(0);
        }

        return this;
    }

    protected ensure_size(size: number): void {
        if (size > this.bytes_left) {
            throw new Error(`${size} Bytes required but only ${this.bytes_left} available.`);
        }
    }
}
