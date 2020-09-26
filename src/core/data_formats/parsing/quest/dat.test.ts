import { Endianness } from "../../block/Endianness";
import { prs_decompress } from "../../compression/prs/decompress";
import { BufferCursor } from "../../block/cursor/BufferCursor";
import { ResizableBlockCursor } from "../../block/cursor/ResizableBlockCursor";
import { parse_dat, write_dat } from "./dat";
import { readFileSync } from "fs";
import { unwrap } from "../../../Result";
import { pw_test } from "../../../../../test/src/utils";

/**
 * Parse a file, convert the resulting structure to DAT again and check whether the end result is equal to the original.
 */
test(
    "parse_dat and write_dat",
    pw_test({}, () => {
        const orig_buffer = readFileSync("test/resources/quest118_e.dat");
        const orig_dat = unwrap(prs_decompress(new BufferCursor(orig_buffer, Endianness.Little)));
        const test_dat = new ResizableBlockCursor(write_dat(parse_dat(orig_dat)));
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
    }),
);

/**
 * Parse a file, modify the resulting structure, convert it to DAT again and check whether the end result is equal to the original except for the bytes that should be changed.
 */
test(
    "parse, modify and write DAT",
    pw_test({}, () => {
        const orig_buffer = readFileSync("./test/resources/quest118_e.dat");
        const orig_dat = unwrap(prs_decompress(new BufferCursor(orig_buffer, Endianness.Little)));
        const test_parsed = parse_dat(orig_dat);
        orig_dat.seek_start(0);

        const test_obj_array = new Float32Array(test_parsed.objs[9].data);
        test_obj_array[4] = 13;
        test_obj_array[5] = 17;
        test_obj_array[6] = 19;

        const test_dat = new ResizableBlockCursor(write_dat(test_parsed));

        expect(test_dat.size).toBe(orig_dat.size);

        while (orig_dat.bytes_left) {
            if (orig_dat.position === 16 + 9 * 68 + 16) {
                orig_dat.seek(12);

                expect(test_dat.f32()).toBe(13);
                expect(test_dat.f32()).toBe(17);
                expect(test_dat.f32()).toBe(19);
            } else {
                const test_byte = test_dat.u8();
                const orig_byte = orig_dat.u8();

                if (test_byte !== orig_byte) {
                    throw new Error(
                        `Byte ${
                            test_dat.position - 1
                        } didn't match, expected ${orig_byte}, got ${test_byte}.`,
                    );
                }
            }
        }
    }),
);
