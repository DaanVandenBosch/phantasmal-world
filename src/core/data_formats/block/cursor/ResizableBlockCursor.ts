import { Endianness } from "../Endianness";
import { AbstractWritableCursor } from "./AbstractWritableCursor";
import { ResizableBlock } from "../ResizableBlock";
import { Cursor } from "./Cursor";
import { Vec2, Vec3 } from "../../vector";

export class ResizableBlockCursor extends AbstractWritableCursor {
    private readonly block: ResizableBlock;

    private _size: number;

    get size(): number {
        return this._size;
    }

    set size(size: number) {
        if (size > this._size) {
            this.ensure_size(size - this.position);
        } else {
            this._size = size;
        }
    }

    get endianness(): Endianness {
        return this.block.endianness;
    }

    /**
     * Also sets the underlying block's endianness.
     */
    set endianness(endianness: Endianness) {
        this.block.endianness = endianness;
    }

    /**
     * @param block The block to read from and write to.
     * @param offset The start offset of the part that will be read from.
     * @param size The size of the part that will be read from.
     */
    constructor(block: ResizableBlock, offset: number = 0, size: number = block.size - offset) {
        if (offset < 0 || offset > block.size) {
            throw new Error(`Offset ${offset} is out of bounds.`);
        }

        if (size < 0 || offset + size > block.size) {
            throw new Error(`Size ${size} is out of bounds.`);
        }

        super(offset);

        this.block = block;
        this._size = size;
    }

    u8(): number {
        const r = this.block.get_u8(this.absolute_position);
        this._position++;
        return r;
    }

    u16(): number {
        const r = this.block.get_u16(this.absolute_position);
        this._position += 2;
        return r;
    }

    u32(): number {
        const r = this.block.get_u32(this.absolute_position);
        this._position += 4;
        return r;
    }

    i8(): number {
        const r = this.block.get_i8(this.absolute_position);
        this._position++;
        return r;
    }

    i16(): number {
        const r = this.block.get_i16(this.absolute_position);
        this._position += 2;
        return r;
    }

    i32(): number {
        const r = this.block.get_i32(this.absolute_position);
        this._position += 4;
        return r;
    }

    f32(): number {
        const r = this.block.get_f32(this.absolute_position);
        this._position += 4;
        return r;
    }

    u8_array(n: number): number[] {
        this.check_size(n);

        const array = [];

        for (let i = 0; i < n; ++i) {
            array.push(this.block.get_u8(this.absolute_position));
            this._position++;
        }

        return array;
    }

    u16_array(n: number): number[] {
        this.check_size(2 * n);

        const array = [];

        for (let i = 0; i < n; ++i) {
            array.push(this.block.get_u16(this.absolute_position));
            this._position += 2;
        }

        return array;
    }

    u32_array(n: number): number[] {
        this.check_size(4 * n);

        const array = [];

        for (let i = 0; i < n; ++i) {
            array.push(this.block.get_u32(this.absolute_position));
            this._position += 4;
        }

        return array;
    }

    i32_array(n: number): number[] {
        this.check_size(4 * n);

        const array = [];

        for (let i = 0; i < n; ++i) {
            array.push(this.block.get_i32(this.absolute_position));
            this._position += 4;
        }

        return array;
    }

    take(size: number): ResizableBlockCursor {
        const offset = this.absolute_position;
        const wrapper = new ResizableBlockCursor(this.block, offset, size);
        this._position += size;
        return wrapper;
    }

    array_buffer(size: number = this.size - this.position): ArrayBuffer {
        const r = this.block.get_array_buffer(this.absolute_position, size);
        this._position += size;
        return r;
    }

    copy_to_uint8_array(array: Uint8Array, size: number = this.size - this.position): this {
        array.set(this.block.uint8_view(this.absolute_position, size));
        this._position += size;
        return this;
    }

    write_u8(value: number): this {
        this.ensure_size(1);
        this.block.set_u8(this.absolute_position, value);
        this._position++;
        return this;
    }

    write_u16(value: number): this {
        this.ensure_size(2);
        this.block.set_u16(this.absolute_position, value);
        this._position += 2;
        return this;
    }

    write_u32(value: number): this {
        this.ensure_size(4);
        this.block.set_u32(this.absolute_position, value);
        this._position += 4;
        return this;
    }

    write_i8(value: number): this {
        this.ensure_size(1);
        this.block.set_i8(this.absolute_position, value);
        this._position++;
        return this;
    }

    write_i16(value: number): this {
        this.ensure_size(2);
        this.block.set_i16(this.absolute_position, value);
        this._position += 2;
        return this;
    }

    write_i32(value: number): this {
        this.ensure_size(4);
        this.block.set_i32(this.absolute_position, value);
        this._position += 4;
        return this;
    }

    write_f32(value: number): this {
        this.ensure_size(4);
        this.block.set_f32(this.absolute_position, value);
        this._position += 4;
        return this;
    }

    write_u8_array(array: ArrayLike<number>): this {
        this.ensure_size(array.length);
        return super.write_u8_array(array);
    }

    write_u16_array(array: ArrayLike<number>): this {
        this.ensure_size(2 * array.length);
        return super.write_u16_array(array);
    }

    write_u32_array(array: ArrayLike<number>): this {
        this.ensure_size(4 * array.length);
        return super.write_u32_array(array);
    }

    write_i32_array(array: ArrayLike<number>): this {
        this.ensure_size(4 * array.length);
        return super.write_i32_array(array);
    }

    write_vec2_f32(value: Vec2): this {
        this.ensure_size(8);
        return super.write_vec2_f32(value);
    }

    write_vec3_f32(value: Vec3): this {
        this.ensure_size(12);
        return super.write_vec3_f32(value);
    }

    write_cursor(other: Cursor): this {
        const size = other.size - other.position;
        this.ensure_size(size);

        other.copy_to_uint8_array(this.block.uint8_view(this.absolute_position, size), size);

        this._position += size;
        return this;
    }

    write_string_ascii(str: string, byte_length: number): this {
        this.ensure_size(byte_length);
        return super.write_string_ascii(str, byte_length);
    }

    write_string_utf16(str: string, byte_length: number): this {
        this.ensure_size(byte_length);
        return super.write_string_utf16(str, byte_length);
    }

    private ensure_size(size: number): void {
        const needed = this._position + size - this._size;

        if (needed > 0) {
            this._size += needed;

            if (this.block.size < this.offset + this._size) {
                this.block.size = this.offset + this._size;
            }
        }
    }
}
