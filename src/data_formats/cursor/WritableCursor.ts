import { Cursor } from "./Cursor";
import { Vec2, Vec3 } from "../vector";

/**
 * A cursor for reading and writing binary data.
 */
export interface WritableCursor extends Cursor {
    size: number;

    /**
     * Writes an unsigned 8-bit integer and increments position by 1.
     */
    write_u8(value: number): this;

    /**
     * Writes an unsigned 16-bit integer and increments position by 2.
     */
    write_u16(value: number): this;

    /**
     * Writes an unsigned 32-bit integer and increments position by 4.
     */
    write_u32(value: number): this;

    /**
     * Writes a signed 32-bit integer and increments position by 4.
     */
    write_i32(value: number): this;

    /**
     * Writes a 32-bit floating point number and increments position by 4.
     */
    write_f32(value: number): this;

    /**
     * Writes an array of unsigned 8-bit integers and increments position by the array's length.
     */
    write_u8_array(array: number[]): this;

    /**
     * Writes an array of unsigned 16-bit integers and increments position by twice the array's length.
     */
    write_u16_array(array: number[]): this;

    /**
     * Writes an array of unsigned 32-bit integers and increments position by four times the array's length.
     */
    write_u32_array(array: number[]): this;

    /**
     * Writes two 32-bit floating point numbers and increments position by 8.
     */
    write_vec2_f32(value: Vec2): this;

    /**
     * Writes three 32-bit floating point numbers and increments position by 12.
     */
    write_vec3_f32(value: Vec3): this;

    /**
     * Writes the contents of the given cursor from its position to its end. Increments this cursor's and the given cursor's position by the size of the given cursor.
     */
    write_cursor(other: Cursor): this;

    /**
     * Writes byte_length characters of str. If str is shorter than byte_length, nul bytes will be inserted until byte_length bytes have been written.
     */
    write_string_ascii(str: string, byte_length: number): this;

    /**
     * Writes characters of str without writing more than byte_length bytes. If less than byte_length bytes can be written this way, nul bytes will be inserted until byte_length bytes have been written.
     */
    write_string_utf16(str: string, byte_length: number): this;
}
