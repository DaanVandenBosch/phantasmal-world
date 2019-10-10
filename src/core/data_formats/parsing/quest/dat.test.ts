import { Endianness } from "../../Endianness";
import { prs_decompress } from "../../compression/prs/decompress";
import { BufferCursor } from "../../cursor/BufferCursor";
import { ResizableBufferCursor } from "../../cursor/ResizableBufferCursor";
import { DatFile, parse_dat, write_dat } from "./dat";
import { readFileSync } from "fs";

/**
 * Parse a file, convert the resulting structure to DAT again and check whether the end result is equal to the original.
 */
test("parse_dat and write_dat", () => {
    const orig_buffer = readFileSync("test/resources/quest118_e.dat");
    const orig_dat = prs_decompress(new BufferCursor(orig_buffer, Endianness.Little));
    const test_dat = new ResizableBufferCursor(write_dat(parse_dat(orig_dat)), Endianness.Little);
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
test("parse, modify and write DAT", () => {
    const orig_buffer = readFileSync("./test/resources/quest118_e.dat");
    const orig_dat = prs_decompress(new BufferCursor(orig_buffer, Endianness.Little));
    const test_parsed = parse_dat(orig_dat);
    orig_dat.seek_start(0);

    const test_updated: DatFile = {
        ...test_parsed,
        objs: test_parsed.objs.map((obj, i) => {
            if (i === 9) {
                return {
                    ...obj,
                    position: {
                        x: 13,
                        y: 17,
                        z: 19,
                    },
                };
            } else {
                return obj;
            }
        }),
    };

    const test_dat = new ResizableBufferCursor(write_dat(test_updated), Endianness.Little);

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
