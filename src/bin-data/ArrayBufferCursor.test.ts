import { ArrayBufferCursor } from './ArrayBufferCursor';

test('simple properties and invariants', () => {
    const cursor = new ArrayBufferCursor(10, true);

    expect(cursor.size).toBe(cursor.position + cursor.bytesLeft);
    expect(cursor.size).toBeLessThanOrEqual(cursor.capacity);
    expect(cursor.size).toBe(0);
    expect(cursor.capacity).toBe(10);
    expect(cursor.position).toBe(0);
    expect(cursor.bytesLeft).toBe(0);
    expect(cursor.littleEndian).toBe(true);

    cursor.writeU8(99).writeU8(99).writeU8(99).writeU8(99);
    cursor.seek(-1);

    expect(cursor.size).toBe(cursor.position + cursor.bytesLeft);
    expect(cursor.size).toBeLessThanOrEqual(cursor.capacity);
    expect(cursor.size).toBe(4);
    expect(cursor.capacity).toBe(10);
    expect(cursor.position).toBe(3);
    expect(cursor.bytesLeft).toBe(1);
    expect(cursor.littleEndian).toBe(true);
});

test('correct byte order handling', () => {
    const buffer = new Uint8Array([1, 2, 3, 4]).buffer;

    expect(new ArrayBufferCursor(buffer, false).u32()).toBe(0x01020304);
    expect(new ArrayBufferCursor(buffer, true).u32()).toBe(0x04030201);
});

test('reallocation of internal buffer when necessary', () => {
    const cursor = new ArrayBufferCursor(3, true);
    cursor.writeU8(99).writeU8(99).writeU8(99).writeU8(99);

    expect(cursor.size).toBe(4);
    expect(cursor.capacity).toBeGreaterThanOrEqual(4);
    expect(cursor.buffer.byteLength).toBeGreaterThanOrEqual(4);
});

function testIntegerRead(methodName: string) {
    test(methodName, () => {
        const bytes = parseInt(methodName.replace(/^[iu](\d+)$/, '$1'), 10) / 8;
        let testNumber1 = 0;
        let testNumber2 = 0;
        // The "false" arrays are for big endian tests and the "true" arrays for little endian tests.
        const testArrays: { [index: string]: number[] } = { false: [], true: [] };

        for (let i = 1; i <= bytes; ++i) {
            // Generates numbers of the form 0x010203...
            testNumber1 <<= 8;
            testNumber1 |= i;
            testArrays['false'].push(i);
            testArrays['true'].unshift(i);
        }

        for (let i = bytes + 1; i <= 2 * bytes; ++i) {
            testNumber2 <<= 8;
            testNumber2 |= i;
            testArrays['false'].push(i);
            testArrays['true'].splice(bytes, 0, i);
        }

        for (const littleEndian of [false, true]) {
            const cursor = new ArrayBufferCursor(
                new Uint8Array(testArrays[String(littleEndian)]).buffer, littleEndian);

            expect((cursor as any)[methodName]()).toBe(testNumber1);
            expect(cursor.position).toBe(bytes);

            expect((cursor as any)[methodName]()).toBe(testNumber2);
            expect(cursor.position).toBe(2 * bytes);
        }
    });
}

testIntegerRead('u8');
testIntegerRead('u16');
testIntegerRead('u32');
testIntegerRead('i32');

test('u8Array', () => {
    const cursor = new ArrayBufferCursor(new Uint8Array([1, 2, 3, 4, 5, 6, 7, 8]).buffer, true);

    expect(cursor.u8Array(3)).toEqual([1, 2, 3]);
    expect(cursor.seekStart(2).u8Array(4)).toEqual([3, 4, 5, 6]);
    expect(cursor.seekStart(5).u8Array(3)).toEqual([6, 7, 8]);
});

function testStringRead(methodName: string, charSize: number) {
    test(methodName, () => {
        const charArray = [7, 65, 66, 0, 255, 13];

        for (const littleEndian of [false, true]) {
            const charArrayCopy = [];

            for (const char of charArray) {
                if (littleEndian) charArrayCopy.push(char);

                for (let i = 0; i < charSize - 1; ++i) {
                    charArrayCopy.push(0);
                }

                if (!littleEndian) charArrayCopy.push(char);
            }

            const cursor = new ArrayBufferCursor(
                new Uint8Array(charArrayCopy).buffer, littleEndian);

            cursor.seekStart(charSize);
            expect((cursor as any)[methodName](4 * charSize, true, true)).toBe('AB');
            expect(cursor.position).toBe(5 * charSize);
            cursor.seekStart(charSize);
            expect((cursor as any)[methodName](2 * charSize, true, true)).toBe('AB');
            expect(cursor.position).toBe(3 * charSize);

            cursor.seekStart(charSize);
            expect((cursor as any)[methodName](4 * charSize, true, false)).toBe('AB');
            expect(cursor.position).toBe(4 * charSize);
            cursor.seekStart(charSize);
            expect((cursor as any)[methodName](2 * charSize, true, false)).toBe('AB');
            expect(cursor.position).toBe(3 * charSize);

            cursor.seekStart(charSize);
            expect((cursor as any)[methodName](4 * charSize, false, true)).toBe('AB\0ÿ');
            expect(cursor.position).toBe(5 * charSize);

            cursor.seekStart(charSize);
            expect((cursor as any)[methodName](4 * charSize, false, false)).toBe('AB\0ÿ');
            expect(cursor.position).toBe(5 * charSize);
        }
    });
}

testStringRead('stringAscii', 1);
testStringRead('stringUtf16', 2);

function testIntegerWrite(methodName: string) {
    test(methodName, () => {
        const bytes = parseInt(methodName.replace(/^write[IU](\d+)$/, '$1'), 10) / 8;
        let testNumber1 = 0;
        let testNumber2 = 0;
        // The "false" arrays are for big endian tests and the "true" arrays for little endian tests.
        const testArrays1: { [index: string]: number[] } = { false: [], true: [] };
        const testArrays2: { [index: string]: number[] } = { false: [], true: [] };

        for (let i = 1; i <= bytes; ++i) {
            // Generates numbers of the form 0x010203...
            testNumber1 <<= 8;
            testNumber1 |= i;
            testNumber2 <<= 8;
            testNumber2 |= i + bytes;
            testArrays1['false'].push(i);
            testArrays1['true'].unshift(i);
            testArrays2['false'].push(i + bytes);
            testArrays2['true'].unshift(i + bytes);
        }

        for (const littleEndian of [false, true]) {
            const cursor = new ArrayBufferCursor(0, littleEndian);
            (cursor as any)[methodName](testNumber1);

            expect(cursor.position).toBe(bytes);
            expect(cursor.seekStart(0).u8Array(bytes))
                .toEqual(testArrays1[String(littleEndian)]);
            expect(cursor.position).toBe(bytes);

            (cursor as any)[methodName](testNumber2);

            expect(cursor.position).toBe(2 * bytes);
            expect(cursor.seekStart(0).u8Array(2 * bytes))
                .toEqual(testArrays1[String(littleEndian)].concat(testArrays2[String(littleEndian)]));
        }
    });
}

testIntegerWrite('writeU8');
testIntegerWrite('writeU16');
testIntegerWrite('writeU32');

test('writeF32', () => {
    for (const littleEndian of [false, true]) {
        const cursor = new ArrayBufferCursor(0, littleEndian);
        cursor.writeF32(1337.9001);

        expect(cursor.position).toBe(4);
        expect(cursor.seek(-4).f32()).toBeCloseTo(1337.9001, 4);
        expect(cursor.position).toBe(4);

        cursor.writeF32(103.502);

        expect(cursor.position).toBe(8);
        expect(cursor.seek(-4).f32()).toBeCloseTo(103.502, 3);
    }
});

test('writeU8Array', () => {
    for (const littleEndian of [false, true]) {
        const bytes = 10;
        const cursor = new ArrayBufferCursor(2 * bytes, littleEndian);
        const uint8Array = new Uint8Array(cursor.buffer);
        const testArray1 = [];
        const testArray2 = [];

        for (let i = 1; i <= bytes; ++i) {
            testArray1.push(i);
            testArray2.push(i + bytes);
        }

        cursor.writeU8Array(testArray1);

        expect(cursor.position).toBe(bytes);

        for (let i = 0; i < bytes; ++i) {
            expect(uint8Array[i]).toBe(testArray1[i]);
        }

        cursor.writeU8Array(testArray2);

        expect(cursor.position).toBe(2 * bytes);

        for (let i = 0; i < bytes; ++i) {
            expect(uint8Array[i]).toBe(testArray1[i]);
        }

        for (let i = 0; i < bytes; ++i) {
            expect(uint8Array[i + bytes]).toBe(testArray2[i]);
        }
    }
});
