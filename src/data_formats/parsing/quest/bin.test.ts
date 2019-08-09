import { readFileSync } from "fs";
import { Endianness } from "../../Endianness";
import { prs_decompress } from "../../compression/prs/decompress";
import { ArrayBufferCursor } from "../../cursor/ArrayBufferCursor";
import { BufferCursor } from "../../cursor/BufferCursor";
import { parse_bin, write_bin } from "./bin";

/**
 * Parse a file, convert the resulting structure to BIN again and check whether the end result is equal to the original.
 */
function test_quest(path: string) {
    const orig_buffer = readFileSync(path);
    const orig_bin = prs_decompress(new BufferCursor(orig_buffer, Endianness.Little));
    const test_buffer = write_bin(parse_bin(orig_bin));
    const test_bin = new ArrayBufferCursor(test_buffer, Endianness.Little);

    orig_bin.seek_start(0);
    expect(test_bin.size).toBe(orig_bin.size);

    let matching_bytes = 0;

    while (orig_bin.bytes_left) {
        const test_byte = test_bin.u8();
        const orig_byte = orig_bin.u8();

        if (test_byte !== orig_byte) {
            throw new Error(
                `Byte ${matching_bytes} didn't match, expected ${orig_byte}, got ${test_byte}.`
            );
        }

        matching_bytes++;
    }

    expect(matching_bytes).toBe(orig_bin.size);
}

test("parse_bin and write_bin with quest118_e.bin", () => {
    test_quest("test/resources/quest118_e.bin");
});

test("parse_bin and write_bin with quest27_e.bin", () => {
    test_quest("test/resources/quest27_e.bin");
});
