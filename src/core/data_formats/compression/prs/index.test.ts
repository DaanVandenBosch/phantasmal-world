import { readFileSync } from "fs";
import { Endianness } from "../../Endianness";
import { ArrayBufferCursor } from "../../cursor/ArrayBufferCursor";
import { BufferCursor } from "../../cursor/BufferCursor";
import { prs_compress_js } from "./compress";
import { prs_decompress_js } from "./decompress";

function test_with_bytes(bytes: number[], expected_compressed_size: number): void {
    const cursor = new ArrayBufferCursor(new Uint8Array(bytes).buffer, Endianness.Little);
    const compressed_cursor = prs_compress_js(cursor);

    expect(compressed_cursor.size).toBe(expected_compressed_size);

    const test_cursor = prs_decompress_js(compressed_cursor);
    cursor.seek_start(0);

    expect(test_cursor.size).toBe(cursor.size);

    while (cursor.bytes_left) {
        if (cursor.u8() !== test_cursor.u8()) {
            cursor.seek(-1);
            test_cursor.seek(-1);
            break;
        }
    }

    expect(test_cursor.position).toBe(test_cursor.size);
}

test("PRS compression and decompression, best case", () => {
    // Compression factor: 0.048
    test_with_bytes(new Array(10000).fill(128), 475);
});

test("PRS compression and decompression, worst case", () => {
    const prng = new Prng();

    // Compression factor: 1.125
    test_with_bytes(
        new Array(10000).fill(0).map(() => prng.next_integer(0, 255)),
        11253,
    );
});

test("PRS compression and decompression, typical case", () => {
    const prng = new Prng();
    const pattern = [0, 0, 2, 0, 3, 0, 5, 0, 0, 0, 7, 9, 11, 13, 0, 0];
    const arrays = new Array(1000)
        .fill(pattern)
        .map(array => array.map((e: number) => e + prng.next_integer(0, 10)));
    // eslint-disable-next-line prefer-spread
    const flattened_array = [].concat.apply([], arrays);

    // Compression factor: 0.933
    test_with_bytes(flattened_array, 14924);
});

test("PRS compression and decompression, 0 bytes", () => {
    test_with_bytes([], 3);
});

test("PRS compression and decompression, 1 byte", () => {
    test_with_bytes([111], 4);
});

test("PRS compression and decompression, 2 bytes", () => {
    test_with_bytes([111, 224], 5);
});

test("PRS compression and decompression, 3 bytes", () => {
    test_with_bytes([56, 237, 158], 6);
});

test("PRS compression and decompression of quest118_e.bin", () => {
    const buffer = readFileSync("test/resources/quest118_e.bin");
    const orig = prs_decompress_js(new BufferCursor(buffer, Endianness.Little));
    const test = prs_decompress_js(prs_compress_js(orig));
    orig.seek_start(0);

    expect(test.size).toBe(orig.size);

    let matching_bytes = 0;

    while (orig.bytes_left) {
        const test_byte = test.u8();
        const orig_byte = orig.u8();

        if (test_byte !== orig_byte) {
            throw new Error(
                `Byte ${matching_bytes} didn't match, expected ${orig_byte}, got ${test_byte}.`,
            );
        }

        matching_bytes++;
    }

    expect(matching_bytes).toBe(orig.size);
});

class Prng {
    seed = 1;

    next(): number {
        const x = Math.sin(this.seed++) * 10000;
        return x - Math.floor(x);
    }

    next_integer(min: number, max: number): number {
        return Math.floor(this.next() * (max + 1 - min)) + min;
    }
}
