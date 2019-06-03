import * as fs from 'fs';
import { ArrayBufferCursor } from '../ArrayBufferCursor';
import * as prs from '../compression/prs';
import { parseBin, writeBin } from './bin';

/**
 * Parse a file, convert the resulting structure to BIN again and check whether the end result is equal to the original.
 */
test('parseBin and writeBin', () => {
    const origBuffer = fs.readFileSync('test/resources/quest118_e.bin').buffer;
    const origBin = prs.decompress(new ArrayBufferCursor(origBuffer, true));
    const testBin = writeBin(parseBin(origBin));
    origBin.seekStart(0);

    expect(testBin.size).toBe(origBin.size);

    let match = true;

    while (origBin.bytesLeft) {
        if (testBin.u8() !== origBin.u8()) {
            match = false;
            break;
        }
    }

    expect(match).toBe(true);
});
