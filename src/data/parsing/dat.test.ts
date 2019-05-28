import * as fs from 'fs';
import { ArrayBufferCursor } from '../ArrayBufferCursor';
import * as prs from '../compression/prs';
import { parse_dat, write_dat } from './dat';

/**
 * Parse a file, convert the resulting structure to DAT again and check whether the end result is equal to the original.
 */
test('parse_dat and write_dat', () => {
    const orig_buffer = fs.readFileSync('test/resources/quest118_e.dat').buffer;
    const orig_dat = prs.decompress(new ArrayBufferCursor(orig_buffer, true));
    const test_dat = write_dat(parse_dat(orig_dat));
    orig_dat.seek_start(0);

    expect(test_dat.size).toBe(orig_dat.size);

    let match = true;

    while (orig_dat.bytes_left) {
        if (test_dat.u8() !== orig_dat.u8()) {
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
    const orig_buffer = fs.readFileSync('./test/resources/quest118_e.dat').buffer;
    const orig_dat = prs.decompress(new ArrayBufferCursor(orig_buffer, true));
    const test_parsed = parse_dat(orig_dat);
    orig_dat.seek_start(0);

    test_parsed.objs[9].position.x = 13;
    test_parsed.objs[9].position.y = 17;
    test_parsed.objs[9].position.z = 19;

    const test_dat = write_dat(test_parsed);

    expect(test_dat.size).toBe(orig_dat.size);

    let match = true;

    while (orig_dat.bytes_left) {
        if (orig_dat.position === 16 + 9 * 68 + 16) {
            orig_dat.seek(12);

            expect(test_dat.f32()).toBe(13);
            expect(test_dat.f32()).toBe(17);
            expect(test_dat.f32()).toBe(19);
        } else if (test_dat.u8() !== orig_dat.u8()) {
            match = false;
            break;
        }
    }

    expect(match).toBe(true);
});
