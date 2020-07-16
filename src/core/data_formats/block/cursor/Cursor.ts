import { Endianness } from "../Endianness";
import { Vec3, Vec2 } from "../../vector";

/**
 * A cursor for reading binary data.
 */
export interface Cursor {
    /**
     * The cursor's size. This value will always be non-negative and equal to or smaller than the cursor's capacity.
     */
    readonly size: number;

    /**
     * The position from where bytes will be read or written.
     */
    readonly position: number;

    /**
     * Byte order mode.
     */
    endianness: Endianness;

    readonly bytes_left: number;

    /**
     * Seek forward or backward by a number of bytes.
     *
     * @param offset if positive, seeks forward by offset bytes, otherwise seeks backward by -offset bytes.
     */
    seek(offset: number): this;

    /**
     * Seek forward from the start of the cursor by a number of bytes.
     *
     * @param offset greater or equal to 0 and smaller than size
     */
    seek_start(offset: number): this;

    /**
     * Seek backward from the end of the cursor by a number of bytes.
     *
     * @param offset greater or equal to 0 and smaller than size
     */
    seek_end(offset: number): this;

    /**
     * Reads an unsigned 8-bit integer and increments position by 1.
     */
    u8(): number;

    /**
     * Reads an unsigned 16-bit integer and increments position by 2.
     */
    u16(): number;

    /**
     * Reads an unsigned 32-bit integer and increments position by 4.
     */
    u32(): number;

    /**
     * Reads an signed 8-bit integer and increments position by 1.
     */
    i8(): number;

    /**
     * Reads a signed 16-bit integer and increments position by 2.
     */
    i16(): number;

    /**
     * Reads a signed 32-bit integer and increments position by 4.
     */
    i32(): number;

    /**
     * Reads a 32-bit floating point number and increments position by 4.
     */
    f32(): number;

    /**
     * Reads n unsigned 8-bit integers and increments position by n.
     */
    u8_array(n: number): number[];

    /**
     * Reads n unsigned 16-bit integers and increments position by 2n.
     */
    u16_array(n: number): number[];

    /**
     * Reads n unsigned 32-bit integers and increments position by 4n.
     */
    u32_array(n: number): number[];

    /**
     * Reads n signed 32-bit integers and increments position by 4n.
     */
    i32_array(n: number): number[];

    /**
     * Reads 2 32-bit floating point numbers and increments position by 8.
     */
    vec2_f32(): Vec2;

    /**
     * Reads 3 32-bit floating point numbers and increments position by 12.
     */
    vec3_f32(): Vec3;

    /**
     * Consumes a variable number of bytes.
     *
     * @param size the amount bytes to consume.
     * @returns a write-through view containing size bytes.
     */
    take(size: number): Cursor;

    /**
     * Consumes up to max_byte_length bytes.
     */
    string_ascii(
        max_byte_length: number,
        null_terminated: boolean,
        drop_remaining: boolean,
    ): string;

    /**
     * Consumes up to max_byte_length bytes.
     */
    string_utf16(
        max_byte_length: number,
        null_terminated: boolean,
        drop_remaining: boolean,
    ): string;

    array_buffer(size?: number): ArrayBuffer;

    copy_to_uint8_array(array: Uint8Array, size?: number): this;
}
