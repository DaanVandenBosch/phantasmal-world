import { walk_qst_files } from "../../../../test/src/utils";
import { parse_qst, write_qst } from "./qst";
import { Endianness } from "../../Endianness";
import { BufferCursor } from "../../cursor/BufferCursor";
import { ArrayBufferCursor } from "../../cursor/ArrayBufferCursor";

/**
 * Parse a file, convert the resulting structure to QST again and check whether the end result is equal to the original.
 */
test("parse_qst and write_qst", () => {
    walk_qst_files((_file_path, _file_name, file_content) => {
        const orig_qst = new BufferCursor(file_content, Endianness.Little);
        const orig_quest = parse_qst(orig_qst);

        if (orig_quest) {
            const test_qst = new ArrayBufferCursor(write_qst(orig_quest), Endianness.Little);
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
