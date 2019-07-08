import { ArrayBufferCursor } from "./ArrayBufferCursor";
import { WritableCursor } from "./WritableCursor";
import { ASCII_ENCODER } from ".";
import { Cursor } from "./Cursor";

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
}
