import { Endianness } from "../Endianness";
import { enum_values } from "../../../enums";
import { ResizableBlock } from "../ResizableBlock";
import { ArrayBufferCursor } from "./ArrayBufferCursor";
import { BufferCursor } from "./BufferCursor";
import { Cursor } from "./Cursor";
import { ResizableBlockCursor } from "./ResizableBlockCursor";

/**
 * Run a test on every cursor implementation with every endianness.
 *
 * @param name name of the test, cursor name and endianness will be appended
 * @param bytes an array of bytes which will be used to initialize each cursor.
 * @param run_test the test case, will be called with every cursor-endianness combination.
 */
function test_all(
    name: string,
    bytes: (endianness: Endianness) => number[],
    run_test: (cursor: Cursor, endianness: Endianness) => void,
): void {
    const endiannesses = enum_values<Endianness>(Endianness);

    function block(endianness: Endianness): ResizableBlock {
        const byte_array = bytes(endianness);
        const block = new ResizableBlock(byte_array.length, endianness);
        block.size = byte_array.length;
        block.uint8_view(0, byte_array.length).set(byte_array);
        return block;
    }

    const cursors: [string, Endianness, Cursor][] = [
        ...endiannesses.map(endianness => [
            ArrayBufferCursor.name,
            endianness,
            new ArrayBufferCursor(new Uint8Array(bytes(endianness)).buffer, endianness),
        ]),
        ...endiannesses.map(endianness => [
            BufferCursor.name,
            endianness,
            new BufferCursor(Buffer.from(bytes(endianness)), endianness),
        ]),
        ...endiannesses.map(endianness => [
            ResizableBlockCursor.name,
            endianness,
            new ResizableBlockCursor(block(endianness)),
        ]),
    ] as any;

    for (const [cursor_name, endianness, cursor] of cursors) {
        test(`${name} (${cursor_name} ${Endianness[endianness].toLowerCase()} endian)`, () => {
            run_test(cursor, endianness);
        });
    }
}

test_all(
    "simple properties and invariants",
    () => [0, 1, 2, 3, 4, 5, 6, 7, 8, 9],
    (cursor, endianness) => {
        for (const [seek_to, expected_pos] of [
            [0, 0],
            [3, 3],
            [5, 8],
            [2, 10],
            [-10, 0],
        ]) {
            cursor.seek(seek_to);

            expect(cursor.size).toBe(10);
            expect(cursor.position).toBe(expected_pos);
            expect(cursor.size).toBe(cursor.position + cursor.bytes_left);
            expect(cursor.endianness).toBe(endianness);
        }
    },
);

test_all(
    "correct byte order handling",
    () => [1, 2, 3, 4],
    (cursor, endianness) => {
        if (endianness === Endianness.Little) {
            expect(cursor.u32()).toBe(0x04030201);
        } else {
            expect(cursor.u32()).toBe(0x01020304);
        }
    },
);

/**
 * Reads two integers.
 */
function test_integer_read(method_name: string): void {
    const byte_count = parseInt(method_name.replace(/^[iu](\d+)$/, "$1"), 10) / 8;
    let expected_number_1 = 0;
    let expected_number_2 = 0;
    // Array of bytes per endianness.
    const bytes: [number[], number[]] = [[], []];

    // Generate numbers of the form 0x010203...
    for (let i = 1; i <= byte_count; ++i) {
        expected_number_1 <<= 8;
        expected_number_1 |= i;

        bytes[Endianness.Little].push(byte_count - i + 1);
        bytes[Endianness.Big].push(i);
    }

    for (let i = 1; i <= byte_count; ++i) {
        expected_number_2 <<= 8;
        expected_number_2 |= byte_count + i;

        bytes[Endianness.Little].push(2 * byte_count - i + 1);
        bytes[Endianness.Big].push(byte_count + i);
    }

    test_all(
        method_name,
        endianness => bytes[endianness],
        cursor => {
            expect((cursor as any)[method_name]()).toBe(expected_number_1);
            expect(cursor.position).toBe(byte_count);

            expect((cursor as any)[method_name]()).toBe(expected_number_2);
            expect(cursor.position).toBe(2 * byte_count);
        },
    );
}

test_integer_read("u8");
test_integer_read("u16");
test_integer_read("u32");
test_integer_read("i8");
test_integer_read("i16");
test_integer_read("i32");

test_all(
    "u8_array",
    () => [1, 2, 0xff, 4, 5, 6, 7, 8],
    cursor => {
        expect(cursor.u8_array(3)).toEqual([1, 2, 0xff]);
        expect(cursor.seek_start(2).u8_array(4)).toEqual([0xff, 4, 5, 6]);
        expect(cursor.seek_start(5).u8_array(3)).toEqual([6, 7, 8]);
    },
);

test_all(
    "u16_array",
    () => [1, 1, 2, 2, 0xff, 0xff, 4, 4, 5, 5, 6, 6, 7, 7, 8, 8],
    cursor => {
        expect(cursor.u16_array(3)).toEqual([0x0101, 0x0202, 0xffff]);
        expect(cursor.seek_start(4).u16_array(4)).toEqual([0xffff, 0x0404, 0x0505, 0x0606]);
        expect(cursor.seek_start(10).u16_array(3)).toEqual([0x0606, 0x0707, 0x0808]);
    },
);

test_all(
    "u32_array",
    // prettier-ignore
    () => [1, 1, 1, 1, 2, 2, 2, 2, 0xff, 0xff, 0xff, 0xff, 4, 4, 4, 4, 5, 5, 5, 5, 6, 6, 6, 6, 7, 7, 7, 7, 8, 8, 8, 8],
    cursor => {
        expect(cursor.u32_array(3)).toEqual([0x01010101, 0x02020202, 0xffffffff]);
        expect(cursor.seek_start(8).u32_array(4)).toEqual([
            0xffffffff,
            0x04040404,
            0x05050505,
            0x06060606,
        ]);
        expect(cursor.seek_start(20).u32_array(3)).toEqual([0x06060606, 0x07070707, 0x08080808]);
    },
);

test_all(
    "i32_array",
    // prettier-ignore
    () => [1, 1, 1, 1, 2, 2, 2, 2, 0xff, 0xff, 0xff, 0xff, 4, 4, 4, 4, 5, 5, 5, 5, 6, 6, 6, 6, 7, 7, 7, 7, 8, 8, 8, 8],
    cursor => {
        expect(cursor.i32_array(3)).toEqual([0x01010101, 0x02020202, -1]);
        expect(cursor.seek_start(8).i32_array(4)).toEqual([-1, 0x04040404, 0x05050505, 0x06060606]);
        expect(cursor.seek_start(20).i32_array(3)).toEqual([0x06060606, 0x07070707, 0x08080808]);
    },
);

function test_string_read(method_name: string, char_size: number): void {
    // Array of bytes per endianness.
    const bytes: [number[], number[]] = [[], []];

    for (const endianness of enum_values<Endianness>(Endianness)) {
        const char_array_copy = bytes[endianness];

        for (const char of [7, 65, 66, 0, 255, 13]) {
            if (endianness === Endianness.Little) char_array_copy.push(char);

            for (let i = 0; i < char_size - 1; ++i) {
                char_array_copy.push(0);
            }

            if (endianness === Endianness.Big) char_array_copy.push(char);
        }
    }

    test_all(
        method_name,
        endianness => bytes[endianness],
        cursor => {
            cursor.seek_start(char_size);
            expect((cursor as any)[method_name](4 * char_size, true, true)).toBe("AB");
            expect(cursor.position).toBe(5 * char_size);
            cursor.seek_start(char_size);
            expect((cursor as any)[method_name](2 * char_size, true, true)).toBe("AB");
            expect(cursor.position).toBe(3 * char_size);

            cursor.seek_start(char_size);
            expect((cursor as any)[method_name](4 * char_size, true, false)).toBe("AB");
            expect(cursor.position).toBe(4 * char_size);
            cursor.seek_start(char_size);
            expect((cursor as any)[method_name](2 * char_size, true, false)).toBe("AB");
            expect(cursor.position).toBe(3 * char_size);

            cursor.seek_start(char_size);
            expect((cursor as any)[method_name](4 * char_size, false, true)).toBe("AB\0ÿ");
            expect(cursor.position).toBe(5 * char_size);

            cursor.seek_start(char_size);
            expect((cursor as any)[method_name](4 * char_size, false, false)).toBe("AB\0ÿ");
            expect(cursor.position).toBe(5 * char_size);
        },
    );
}

test_string_read("string_ascii", 1);
test_string_read("string_utf16", 2);
