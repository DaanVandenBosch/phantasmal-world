import { ArrayBufferCursor } from '../../ArrayBufferCursor';
import { compress, decompress } from '../prs';

function test_with_bytes(bytes: number[], expected_compressed_size: number) {
    const cursor = new ArrayBufferCursor(new Uint8Array(bytes).buffer, true);

    for (const byte of bytes) {
        cursor.write_u8(byte);
    }

    cursor.seek_start(0);
    const compressed_cursor = compress(cursor);

    expect(compressed_cursor.size).toBe(expected_compressed_size);

    const test_cursor = decompress(compressed_cursor);
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

test('PRS compression and decompression, best case', () => {
    // Compression factor: 0.018
    test_with_bytes(new Array(1000).fill(128), 18);
});

test('PRS compression and decompression, worst case', () => {
    const prng = new Prng();

    // Compression factor: 1.124
    test_with_bytes(new Array(1000).fill(0).map(_ => prng.next_integer(0, 255)), 1124);
});

test('PRS compression and decompression, typical case', () => {
    const prng = new Prng();
    const pattern = [0, 0, 2, 0, 3, 0, 5, 0, 0, 0, 7, 9, 11, 13, 0, 0];
    const arrays = new Array(100)
        .fill(pattern)
        .map(array => array.map(e => e + prng.next_integer(0, 10)));
    const flattened_array = [].concat.apply([], arrays);

    // Compression factor: 0.834
    test_with_bytes(flattened_array, 1335);
});

test('PRS compression and decompression, 0 bytes', () => {
    test_with_bytes([], 3);
});

test('PRS compression and decompression, 1 byte', () => {
    test_with_bytes([111], 4);
});

test('PRS compression and decompression, 2 bytes', () => {
    test_with_bytes([111, 224], 5);
});

test('PRS compression and decompression, 3 bytes', () => {
    test_with_bytes([56, 237, 158], 6);
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
