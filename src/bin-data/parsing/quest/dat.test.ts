import * as fs from 'fs';
import { ArrayBufferCursor } from '../../ArrayBufferCursor';
import * as prs from '../../compression/prs';
import { parseDat, writeDat } from './dat';

/**
 * Parse a file, convert the resulting structure to DAT again and check whether the end result is equal to the original.
 */
test('parseDat and writeDat', () => {
    const origBuffer = fs.readFileSync('test/resources/quest118_e.dat').buffer;
    const origDat = prs.decompress(new ArrayBufferCursor(origBuffer, true));
    const testDat = writeDat(parseDat(origDat));
    origDat.seekStart(0);

    expect(testDat.size).toBe(origDat.size);

    let match = true;

    while (origDat.bytesLeft) {
        if (testDat.u8() !== origDat.u8()) {
            match = false;
            break;
        }
    }

    expect(match).toBe(true);
});

/**
 * Parse a file, modify the resulting structure, convert it to DAT again and check whether the end result is equal to the original except for the bytes that should be changed.
 */
test('parse, modify and write DAT', () => {
    const origBuffer = fs.readFileSync('./test/resources/quest118_e.dat').buffer;
    const origDat = prs.decompress(new ArrayBufferCursor(origBuffer, true));
    const testParsed = parseDat(origDat);
    origDat.seekStart(0);

    testParsed.objs[9].position.x = 13;
    testParsed.objs[9].position.y = 17;
    testParsed.objs[9].position.z = 19;

    const testDat = writeDat(testParsed);

    expect(testDat.size).toBe(origDat.size);

    let match = true;

    while (origDat.bytesLeft) {
        if (origDat.position === 16 + 9 * 68 + 16) {
            origDat.seek(12);

            expect(testDat.f32()).toBe(13);
            expect(testDat.f32()).toBe(17);
            expect(testDat.f32()).toBe(19);
        } else if (testDat.u8() !== origDat.u8()) {
            match = false;
            break;
        }
    }

    expect(match).toBe(true);
});
