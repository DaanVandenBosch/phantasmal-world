import * as fs from 'fs';
import { BufferCursor } from '../../BufferCursor';
import * as prs from '../../compression/prs';
import { parseBin, writeBin } from './bin';

/**
 * Parse a file, convert the resulting structure to BIN again and check whether the end result is equal to the original.
 */
test('parseBin and writeBin', () => {
    const origBuffer = fs.readFileSync('test/resources/quest118_e.bin').buffer;
    const origBin = prs.decompress(new BufferCursor(origBuffer, true));
    const testBin = writeBin(parseBin(origBin));
    origBin.seek_start(0);

    expect(testBin.size).toBe(origBin.size);

    let match = true;

    while (origBin.bytes_left) {
        if (testBin.u8() !== origBin.u8()) {
            match = false;
            break;
        }
    }

    expect(match).toBe(true);
});
