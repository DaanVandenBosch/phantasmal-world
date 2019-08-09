import { Endianness } from "../Endianness";
import { enum_values } from "../../enums";
import { ResizableBuffer } from "../ResizableBuffer";
import { ArrayBufferCursor } from "./ArrayBufferCursor";
import { ResizableBufferCursor } from "./ResizableBufferCursor";
import { WritableCursor } from "./WritableCursor";

/**
 * Run a test on every writable cursor implementation with every endianness.
 *
 * @param name name of the test, cursor name and endianness will be appended
 * @param bytes an array of bytes which will be used to initialize each cursor.
 * @param run_test the test case, will be called with every cursor-endianness combination.
 */
function test_all(
    name: string,
    bytes: (endianness: Endianness) => number[],
    run_test: (cursor: WritableCursor, endianness: Endianness) => void
): void {
    const endiannesses = enum_values<Endianness>(Endianness);

    function rbuf(endianness: Endianness): ResizableBuffer {
        const byte_array = bytes(endianness);
        const rbuf = new ResizableBuffer(byte_array.length);
        rbuf.size = byte_array.length;

        for (let i = 0; i < byte_array.length; i++) {
            rbuf.view.setUint8(i, byte_array[i]);
        }

        return rbuf;
    }

    const cursors: [string, Endianness, WritableCursor][] = [
        ...endiannesses.map(endianness => [
            ArrayBufferCursor.name,
            endianness,
            new ArrayBufferCursor(new Uint8Array(bytes(endianness)).buffer, endianness),
        ]),
        ...endiannesses.map(endianness => [
            ResizableBufferCursor.name,
            endianness,
            new ResizableBufferCursor(rbuf(endianness), endianness),
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
        expect(cursor.position).toBe(0);

        cursor
            .write_u8(99)
            .write_u8(99)
            .write_u8(99)
            .write_u8(99);
        cursor.seek(-1);

        expect(cursor.size).toBe(cursor.position + cursor.bytes_left);
        expect(cursor.size).toBe(10);
        expect(cursor.position).toBe(3);
        expect(cursor.bytes_left).toBe(7);
        expect(cursor.endianness).toBe(endianness);
    }
);

/**
 * Writes and reads two integers.
 */
function test_integer_write(method_name: string): void {
    const byte_count = parseInt(method_name.replace(/^write_[iu](\d+)$/, "$1"), 10) / 8;
    let expected_number_1 = 0;
    let expected_number_2 = 0;

    // Generate numbers of the form 0x010203...
    for (let i = 1; i <= byte_count; ++i) {
        expected_number_1 <<= 8;
        expected_number_1 |= i;
    }

    for (let i = 1; i <= byte_count; ++i) {
        expected_number_2 <<= 8;
        expected_number_2 |= byte_count + i;
    }

    const read_method_name = method_name.replace("write_", "");

    test_all(
        method_name,
        () => [0, 0, 0, 0, 0, 0, 0, 0],
        cursor => {
            (cursor as any)[method_name](expected_number_1);
            (cursor as any)[method_name](expected_number_2);

            expect(cursor.position).toBe(2 * byte_count);

            cursor.seek_start(0);

            expect((cursor as any)[read_method_name]()).toBe(expected_number_1);
            expect((cursor as any)[read_method_name]()).toBe(expected_number_2);
        }
    );
}

test_integer_write("write_u8");
test_integer_write("write_u16");
test_integer_write("write_u32");
test_integer_write("write_i8");
test_integer_write("write_i16");
test_integer_write("write_i32");

/**
 * Writes and reads two floats.
 */
test_all(
    "write_f32",
    () => [0, 0, 0, 0, 0, 0, 0, 0],
    cursor => {
        cursor.write_f32(1337.9001);
        cursor.write_f32(103.502);

        expect(cursor.position).toBe(8);

        cursor.seek_start(0);

        expect(cursor.f32()).toBeCloseTo(1337.9001, 4);
        expect(cursor.f32()).toBeCloseTo(103.502, 3);

        expect(cursor.position).toBe(8);
    }
);

test_all(
    "write_u8_array",
    () => new Array<number>(20).fill(0),
    cursor => {
        const test_array_1 = [];
        const test_array_2 = [];

        for (let i = 1; i <= 10; ++i) {
            test_array_1.push(i);
            test_array_2.push(i + 10);
        }

        cursor.write_u8_array(test_array_1);

        expect(cursor.position).toBe(10);

        cursor.write_u8_array(test_array_2);

        expect(cursor.position).toBe(20);

        cursor.seek_start(0);

        for (let i = 0; i < 10; ++i) {
            expect(cursor.u8()).toBe(test_array_1[i]);
        }

        for (let i = 0; i < 10; ++i) {
            expect(cursor.u8()).toBe(test_array_2[i]);
        }

        expect(cursor.position).toBe(20);
    }
);

test_all(
    "write, seek backwards then take",
    () => new Array<number>(16).fill(0),
    cursor => {
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
    }
);
