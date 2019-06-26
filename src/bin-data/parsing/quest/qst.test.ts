import { BufferCursor } from '../../BufferCursor';
import { parseQst, writeQst } from './qst';
import { walkQstFiles  } from '../../../../test/src/utils';

/**
 * Parse a file, convert the resulting structure to QST again and check whether the end result is equal to the original.
 */
test('parseQst and writeQst', () => {
    walkQstFiles((_filePath, _fileName, fileContent) => {
        const origQst = new BufferCursor(fileContent.buffer, true);
        const origQuest = parseQst(origQst);

        if (origQuest) {
            const testQst = writeQst(origQuest);
            origQst.seek_start(0);

            expect(testQst.size).toBe(origQst.size);

            let match = true;

            while (origQst.bytes_left) {
                if (testQst.u8() !== origQst.u8()) {
                    match = false;
                    break;
                }
            }

            expect(match).toBe(true);
        }
    });
});
