import { Block } from "./Block";

/**
 * Represents a mutable, continuous block of bytes.
 */
export interface WritableBlock extends Block {
    readonly size: number;

    /**
     * Writes an unsigned 8-bit integer at the given offset.
     */
    set_u8(offset: number, value: number): this;

    /**
     * Writes an unsigned 16-bit integer at the given offset.
     */
    set_u16(offset: number, value: number): this;

    /**
     * Writes an unsigned 32-bit integer at the given offset.
     */
    set_u32(offset: number, value: number): this;

    /**
     * Writes a signed 8-bit integer at the given offset.
     */
    set_i8(offset: number, value: number): this;

    /**
     * Writes a signed 16-bit integer at the given offset.
     */
    set_i16(offset: number, value: number): this;

    /**
     * Writes a signed 32-bit integer at the given offset.
     */
    set_i32(offset: number, value: number): this;

    /**
     * Writes a 32-bit floating point number at the given offset.
     */
    set_f32(offset: number, value: number): this;

    /**
     * Writes 0 bytes to the entire block.
     */
    zero(): this;
}
