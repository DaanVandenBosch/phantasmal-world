import { readFileSync } from "fs";
import { Endianness } from "../../../core/data_formats/Endianness";
import { prs_decompress } from "../../../core/data_formats/compression/prs/decompress";
import { ArrayBufferCursor } from "../../../core/data_formats/cursor/ArrayBufferCursor";
import { BufferCursor } from "../../../core/data_formats/cursor/BufferCursor";
import { parse_bin, write_bin } from "../../../core/data_formats/parsing/quest/bin";
import { assemble } from "./assembly";
import { disassemble } from "./disassembly";

// Roundtrip test.
test("assembling dissambled object code with manual stack management should result in the same object code", () => {
    const orig_buffer = readFileSync("test/resources/quest27_e.bin");
    const orig_bytes = prs_decompress(new BufferCursor(orig_buffer, Endianness.Little));
    const bin = parse_bin(orig_bytes);

    const { object_code, warnings, errors } = assemble(disassemble(bin.object_code, true), true);

    expect(errors).toEqual([]);
    expect(warnings).toEqual([]);

    bin.object_code.splice(0, bin.object_code.length, ...object_code);

    const test_bytes = new ArrayBufferCursor(write_bin(bin), Endianness.Little);

    orig_bytes.seek_start(0);
    expect(test_bytes.size).toBe(orig_bytes.size);

    let matching_bytes = 0;

    while (orig_bytes.bytes_left) {
        const test_byte = test_bytes.u8();
        const orig_byte = orig_bytes.u8();

        if (test_byte !== orig_byte) {
            throw new Error(
                `Byte ${matching_bytes} didn't match, expected ${orig_byte}, got ${test_byte}.`,
            );
        }

        matching_bytes++;
    }

    expect(matching_bytes).toBe(orig_bytes.size);
});

// Roundtrip test.
test("disassembling assembled assembly code with automatic stack management should result the same assembly code", () => {
    const orig_buffer = readFileSync("test/resources/quest27_e.bin");
    const orig_bytes = prs_decompress(new BufferCursor(orig_buffer, Endianness.Little));
    const orig_asm = disassemble(parse_bin(orig_bytes).object_code);

    const { object_code, warnings, errors } = assemble(orig_asm);

    expect(errors).toEqual([]);
    expect(warnings).toEqual([]);

    const test_asm = disassemble(object_code);
    const len = Math.min(orig_asm.length, test_asm.length);

    for (let i = 0; i < len; i++) {
        expect(test_asm[i]).toBe(orig_asm[i]);
    }

    expect(test_asm.length).toBe(orig_asm.length);
});
