import * as fs from 'fs';
import { ArrayBufferCursor } from '../ArrayBufferCursor';
import * as prs from '../compression/prs';
import { parse_bin, write_bin } from './bin';

/**
 * Parse a file, convert the resulting structure to BIN again and check whether the end result is equal to the original.
 */
test('parse_bin and write_bin', () => {
    const orig_buffer = fs.readFileSync('test/resources/quest118_e.bin').buffer;
    const orig_bin = prs.decompress(new ArrayBufferCursor(orig_buffer, true));
    const test_bin = write_bin(parse_bin(orig_bin));
    orig_bin.seek_start(0);

    expect(test_bin.size).toBe(orig_bin.size);

    let match = true;

    while (orig_bin.bytes_left) {
        if (test_bin.u8() !== orig_bin.u8()) {
            match = false;
            break;
        }
    }

    expect(match).toBe(true);
});
