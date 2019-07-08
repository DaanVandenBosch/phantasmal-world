import * as fs from "fs";
import * as prs from "../../compression/prs";
import { parse_bin, write_bin } from "./bin";
import { Endianness } from "../..";
import { BufferCursor } from "../../cursor/BufferCursor";
import { ArrayBufferCursor } from "../../cursor/ArrayBufferCursor";

/**
 * Parse a file, convert the resulting structure to BIN again and check whether the end result is equal to the original.
 */
test("parse_bin and write_bin", () => {
    const orig_buffer = fs.readFileSync("test/resources/quest118_e.bin");
    const orig_bin = prs.decompress(new BufferCursor(orig_buffer, Endianness.Little));
    const test_bin = new ArrayBufferCursor(write_bin(parse_bin(orig_bin)), Endianness.Little);
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
