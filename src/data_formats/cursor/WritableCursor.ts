import { Cursor } from "./Cursor";

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
     * Writes the contents of the given cursor from its position to its end. Increments this cursor's and the given cursor's position by the size of the given cursor.
     */
    write_cursor(other: Cursor): this;

    /**
     * Writes byte_length characters of str. If str is shorter than byte_length, nul bytes will be inserted until byte_length bytes have been written.
     */
    write_string_ascii(str: string, byte_length: number): this;
}
