import { Endianness } from "../Endianness";
import { ResizableBlock } from "../ResizableBlock";
import { ResizableBlockCursor } from "./ResizableBlockCursor";

/**
 * Writes two integers to a cursor backed with a buffer of size 0.
 * Tests that size is automatically increased.
 */
function test_integer_write(method_name: string): void {
    test(`${method_name} increases buffer and cursor size`, () => {
        const byte_count = parseInt(method_name.replace(/^write_[iu](\d+)$/, "$1"), 10) / 8;
        const expected_number_1 = 98749;
        const expected_number_2 = 7348942;

        const block = new ResizableBlock(8, Endianness.Little);
        const cursor = new ResizableBlockCursor(block);

        expect(block.size).toBe(0);
        expect(cursor.size).toBe(0);

        (cursor as any)[method_name](expected_number_1);
        (cursor as any)[method_name](expected_number_2);

        expect(block.size).toBe(2 * byte_count);
        expect(cursor.position).toBe(2 * byte_count);
        expect(cursor.size).toBe(2 * byte_count);
    });
}

test_integer_write("write_u8");
test_integer_write("write_u16");
test_integer_write("write_u32");
test_integer_write("write_i32");

test("write, seek backwards then take", () => {
    const cursor = new ResizableBlockCursor(new ResizableBlock(0, Endianness.Little));
    cursor.write_u32(1).write_u32(2).write_u32(3).write_u32(4);

    cursor.seek(-8);
    const new_cursor = cursor.take(8);

    expect(new_cursor.size).toBe(8);
    expect(new_cursor.position).toBe(0);
    expect(new_cursor.u32()).toBe(3);
    expect(new_cursor.u32()).toBe(4);
});
