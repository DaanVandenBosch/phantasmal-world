import { ArrayBufferCursor } from '../../ArrayBufferCursor';
import { compress, decompress } from '.';

function testWithBytes(bytes: number[], expectedCompressedSize: number) {
    const cursor = new ArrayBufferCursor(new Uint8Array(bytes).buffer, true);

    for (const byte of bytes) {
        cursor.writeU8(byte);
    }

    cursor.seekStart(0);
    const compressedCursor = compress(cursor);

    expect(compressedCursor.size).toBe(expectedCompressedSize);

    const testCursor = decompress(compressedCursor);
    cursor.seekStart(0);

    expect(testCursor.size).toBe(cursor.size);

    while (cursor.bytesLeft) {
        if (cursor.u8() !== testCursor.u8()) {
            cursor.seek(-1);
            testCursor.seek(-1);
            break;
        }
    }

    expect(testCursor.position).toBe(testCursor.size);
}

test('PRS compression and decompression, best case', () => {
    // Compression factor: 0.018
    testWithBytes(new Array(1000).fill(128), 18);
});

test('PRS compression and decompression, worst case', () => {
    const prng = new Prng();

    // Compression factor: 1.124
    testWithBytes(new Array(1000).fill(0).map(_ => prng.nextInteger(0, 255)), 1124);
});

test('PRS compression and decompression, typical case', () => {
    const prng = new Prng();
    const pattern = [0, 0, 2, 0, 3, 0, 5, 0, 0, 0, 7, 9, 11, 13, 0, 0];
    const arrays = new Array(100)
        .fill(pattern)
        .map(array => array.map((e: number) => e + prng.nextInteger(0, 10)));
    const flattenedArray = [].concat.apply([], arrays);

    // Compression factor: 0.834
    testWithBytes(flattenedArray, 1335);
});

test('PRS compression and decompression, 0 bytes', () => {
    testWithBytes([], 3);
});

test('PRS compression and decompression, 1 byte', () => {
    testWithBytes([111], 4);
});

test('PRS compression and decompression, 2 bytes', () => {
    testWithBytes([111, 224], 5);
});

test('PRS compression and decompression, 3 bytes', () => {
    testWithBytes([56, 237, 158], 6);
});

class Prng {
    seed = 1;

    next(): number {
        const x = Math.sin(this.seed++) * 10000;
        return x - Math.floor(x);
    }

    nextInteger(min: number, max: number): number {
        return Math.floor(this.next() * (max + 1 - min)) + min;
    }
}
