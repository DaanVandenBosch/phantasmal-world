import { ASCII_ENCODER } from ".";
import { Vec2, Vec3 } from "../vector";
import { ArrayBufferCursor } from "./ArrayBufferCursor";
import { Cursor } from "./Cursor";
import { WritableCursor } from "./WritableCursor";

/**
 * A cursor for reading and writing from an array buffer or part of an array buffer.
 */
export class WritableArrayBufferCursor extends ArrayBufferCursor implements WritableCursor {
    write_u8(value: number): this {
        this.dv.setUint8(this._position++, value);
        return this;
    }

    write_u16(value: number): this {
        this.dv.setUint16(this.position, value, this.little_endian);
        this._position += 2;
        return this;
    }

    write_u32(value: number): this {
        this.dv.setUint32(this.position, value, this.little_endian);
        this._position += 4;
        return this;
    }

    write_i32(value: number): this {
        this.dv.setInt32(this.position, value, this.little_endian);
        this._position += 4;
        return this;
    }

    write_f32(value: number): this {
        this.dv.setFloat32(this.position, value, this.little_endian);
        this._position += 4;
        return this;
    }

    write_u8_array(array: number[]): this {
        new Uint8Array(this.buffer, this.offset + this.position).set(new Uint8Array(array));
        this._position += array.length;
        return this;
    }

    write_u16_array(array: number[]): this {
        const len = array.length;

        for (let i = 0; i < len; i++) {
            this.write_u16(array[i]);
        }

        return this;
    }

    write_vec2_f32(value: Vec2): this {
        this.dv.setFloat32(this.position, value.x, this.little_endian);
        this.dv.setFloat32(this.position + 4, value.y, this.little_endian);
        this._position += 8;
        return this;
    }

    write_vec3_f32(value: Vec3): this {
        this.dv.setFloat32(this.position, value.x, this.little_endian);
        this.dv.setFloat32(this.position + 4, value.y, this.little_endian);
        this.dv.setFloat32(this.position + 8, value.z, this.little_endian);
        this._position += 12;
        return this;
    }

    write_cursor(other: Cursor): this {
        const size = other.size - other.position;
        other.copy_to_uint8_array(
            new Uint8Array(this.buffer, this.offset + this.position, size),
            size
        );
        this._position += size;
        return this;
    }

    write_string_ascii(str: string, byte_length: number): this {
        const encoded = ASCII_ENCODER.encode(str);
        const encoded_length = Math.min(encoded.byteLength, byte_length);
        let i = 0;

        while (i < encoded_length) {
            this.write_u8(encoded[i++]);
        }

        while (i++ < byte_length) {
            this.write_u8(0);
        }

        return this;
    }

    write_string_utf16(str: string, byte_length: number): this {
        const encoded = this.utf16_encoder.encode(str);
        const encoded_length = Math.min(encoded.byteLength, byte_length);
        let i = 0;

        while (i < encoded_length) {
            this.write_u8(encoded[i++]);
        }

        while (i++ < byte_length) {
            this.write_u8(0);
        }

        return this;
    }
}
