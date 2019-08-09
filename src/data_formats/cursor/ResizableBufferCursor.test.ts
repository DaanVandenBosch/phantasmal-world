import { Endianness } from "../Endianness";
import { ResizableBuffer } from "../ResizableBuffer";
import { ResizableBufferCursor } from "./ResizableBufferCursor";

/**
 * Writes two integers to a cursor backed with a buffer of size 0.
 * Tests that size is automatically increased.
 */
function test_integer_write(method_name: string): void {
    test(`${method_name} increases buffer and cursor size`, () => {
        const byte_count = parseInt(method_name.replace(/^write_[iu](\d+)$/, "$1"), 10) / 8;
        let expected_number_1 = 98749;
        let expected_number_2 = 7348942;

        const buf = new ResizableBuffer(8);
        const cursor = new ResizableBufferCursor(buf, Endianness.Little);

        expect(buf.size).toBe(0);
        expect(cursor.size).toBe(0);

        (cursor as any)[method_name](expected_number_1);
        (cursor as any)[method_name](expected_number_2);

        expect(buf.size).toBe(2 * byte_count);
        expect(cursor.position).toBe(2 * byte_count);
        expect(cursor.size).toBe(2 * byte_count);
    });
}

test_integer_write("write_u8");
test_integer_write("write_u16");
test_integer_write("write_u32");
test_integer_write("write_i32");

test("write, seek backwards then take", () => {
    const cursor = new ResizableBufferCursor(new ResizableBuffer(0), Endianness.Little);
    cursor
        .write_u32(1)
        .write_u32(2)
        .write_u32(3)
        .write_u32(4);

    cursor.seek(-8);
    const new_cursor = cursor.take(8);

    expect(new_cursor.size).toBe(8);
    expect(new_cursor.position).toBe(0);
    expect(new_cursor.u32()).toBe(3);
    expect(new_cursor.u32()).toBe(4);
});
