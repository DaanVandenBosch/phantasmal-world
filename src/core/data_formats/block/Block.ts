import { Endianness } from "./Endianness";

/**
 * Represents a continuous block of bytes.
 */
export interface Block {
    readonly size: number;

    /**
     * Byte order mode.
     */
    endianness: Endianness;

    /**
     * Reads an unsigned 8-bit integer at the given offset.
     */
    get_u8(offset: number): number;

    /**
     * Reads an unsigned 16-bit integer at the given offset.
     */
    get_u16(offset: number): number;

    /**
     * Reads an unsigned 32-bit integer at the given offset.
     */
    get_u32(offset: number): number;

    /**
     * Reads a signed 8-bit integer at the given offset.
     */
    get_i8(offset: number): number;

    /**
     * Reads a signed 16-bit integer at the given offset.
     */
    get_i16(offset: number): number;

    /**
     * Reads a signed 32-bit integer at the given offset.
     */
    get_i32(offset: number): number;

    /**
     * Reads a 32-bit floating point number at the given offset.
     */
    get_f32(offset: number): number;

    /**
     * Reads a UTF-16-encoded string at the given offset.
     */
    get_string_utf16(offset: number, max_byte_length: number, null_terminated: boolean): string;

    /**
     * Reads an array buffer of the given size at the given offset.
     */
    get_array_buffer(offset: number, size: number): ArrayBuffer;

    uint8_view(offset: number, size: number): Uint8Array;
}
