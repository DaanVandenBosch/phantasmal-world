import * as fs from 'fs';
import { ArrayBufferCursor } from '../ArrayBufferCursor';
import * as prs from '../compression/prs';
import { parse_qst, write_qst } from './qst';
import { walk_qst_files } from '../../../test/src/utils';

/**
 * Parse a file, convert the resulting structure to QST again and check whether the end result is equal to the original.
 */
test('parse_qst and write_qst', () => {
    walk_qst_files((file_path, file_name, file_content) => {
        const orig_qst = new ArrayBufferCursor(file_content.buffer, true);
        const orig_quest = parse_qst(orig_qst);

        if (orig_quest) {
            const test_qst = write_qst(orig_quest);
            orig_qst.seek_start(0);

            expect(test_qst.size).toBe(orig_qst.size);

            let match = true;

            while (orig_qst.bytes_left) {
                if (test_qst.u8() !== orig_qst.u8()) {
                    match = false;
                    break;
                }
            }

            expect(match).toBe(true);
        }
    });
});
