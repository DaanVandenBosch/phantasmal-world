import { walk_qst_files } from "../../../../../test/src/utils";
import { parse_qst, write_qst } from "./qst";
import { Endianness } from "../../block/Endianness";
import { BufferCursor } from "../../block/cursor/BufferCursor";
import { ArrayBufferCursor } from "../../block/cursor/ArrayBufferCursor";
import * as fs from "fs";
import { Version } from "./Version";
import { unwrap } from "../../../Result";

test("Parse a GC quest.", () => {
    const buf = fs.readFileSync("test/resources/lost_heat_sword_gc.qst");
    const qst = unwrap(parse_qst(new BufferCursor(buf, Endianness.Little)));

    expect(qst).toBeDefined();
    expect(qst!.version).toBe(Version.GC);
    expect(qst!.online).toBe(true);
    expect(qst!.files.length).toBe(2);
    expect(qst!.files[0].id).toBe(58);
    expect(qst!.files[0].filename).toBe("quest58.bin");
    expect(qst!.files[0].quest_name).toBe("PSO/Lost HEAT SWORD");
    expect(qst!.files[1].id).toBe(58);
    expect(qst!.files[1].filename).toBe("quest58.dat");
    expect(qst!.files[1].quest_name).toBe("PSO/Lost HEAT SWORD");
});

// TODO: some quests aren't written correctly.
const EXCLUDED = [
    "/ep2/shop/gallon.qst",
    "/princ/ep1/",
    "/princ/ep4/",
    "/solo/ep1/04.qst", // Skip because it contains every chuck twice.
    "/fragmentofmemoryen.qst",
    "/lost havoc vulcan.qst",
    "/goodluck.qst",
];

/**
 * Parse a file, convert the resulting structure to QST again and check whether the end result is
 * equal to the original.
 */
walk_qst_files((file_path, file_name, file_content) => {
    if (EXCLUDED.some(e => file_path.includes(e))) {
        return;
    }

    test(`parse_qst and write_qst ${file_name}`, () => {
        const orig_qst = new BufferCursor(file_content, Endianness.Little);
        const orig_quest = unwrap(parse_qst(orig_qst));

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
