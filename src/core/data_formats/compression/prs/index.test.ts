import { readFileSync } from "fs";
import { Endianness } from "../../Endianness";
import { ArrayBufferCursor } from "../../cursor/ArrayBufferCursor";
import { BufferCursor } from "../../cursor/BufferCursor";
import { prs_compress_js } from "./compress";
import { prs_decompress_js } from "./decompress";
import { Cursor } from "../../cursor/Cursor";
import { get_prs_wasm_module } from "./prs_wasm";

type CompressionFunction = (cursor: Cursor) => Cursor;

type CompressionMethod = [string, CompressionFunction, CompressionFunction];

const prs_nop = (cursor: Cursor): Cursor => cursor;

const prs_wasm_module = get_prs_wasm_module();

const should_run_wasm_tests = prs_wasm_module !== undefined;

const prs_compress_wasm = prs_wasm_module
    ? prs_wasm_module.prs_compress_wasm.bind(prs_wasm_module)
    : prs_nop;
const prs_decompress_wasm = prs_wasm_module
    ? prs_wasm_module.prs_decompress_wasm.bind(prs_wasm_module)
    : prs_nop;

const prs_js: CompressionMethod = ["JS", prs_compress_js, prs_decompress_js];
const prs_wasm: CompressionMethod = ["WASM", prs_compress_wasm, prs_decompress_wasm];

/**
 * Helper function that removes wasm tests if wasm module is not available.
 */
function prepare_test_data(js_test_data: any[][], wasm_test_data: any[][]): any[] {
    if (should_run_wasm_tests) {
        return [...js_test_data, ...wasm_test_data];
    } else {
        return js_test_data;
    }
}

function test_with_bytes(
    compress_fn: CompressionFunction,
    decompress_fn: CompressionFunction,
    bytes: number[],
    expected_compressed_size: number,
): void {
    const cursor = new ArrayBufferCursor(new Uint8Array(bytes).buffer, Endianness.Little);
    const compressed_cursor = compress_fn(cursor);

    expect(compressed_cursor.size).toBe(expected_compressed_size);

    const test_cursor = decompress_fn(compressed_cursor);
    cursor.seek_start(0);

    expect(test_cursor.size).toBe(cursor.size);

    while (cursor.bytes_left) {
        if (cursor.u8() !== test_cursor.u8()) {
            cursor.seek(-1);
            test_cursor.seek(-1);
            break;
        }
    }

    expect(test_cursor.position).toBe(test_cursor.size);
}

test.each(prepare_test_data([[...prs_js, 475]], [[...prs_wasm, 134]]))(
    "%s PRS compression and decompression, best case",
    (
        _name: string,
        compress_fn: CompressionFunction,
        decompress_fn: CompressionFunction,
        expected: number,
    ) => {
        test_with_bytes(compress_fn, decompress_fn, new Array(10000).fill(128), expected);
    },
);

test.each(prepare_test_data([[...prs_js, 11253]], [[...prs_wasm, 11250]]))(
    "%s PRS compression and decompression, worst case",
    (
        _name: string,
        compress_fn: CompressionFunction,
        decompress_fn: CompressionFunction,
        expected: number,
    ) => {
        const prng = new Prng();
        test_with_bytes(
            compress_fn,
            decompress_fn,
            new Array(10000).fill(0).map(() => prng.next_integer(0, 255)),
            expected,
        );
    },
);

test.each(prepare_test_data([[...prs_js, 14924]], [[...prs_wasm, 12901]]))(
    "%s PRS compression and decompression, typical case",
    (
        _name: string,
        compress_fn: CompressionFunction,
        decompress_fn: CompressionFunction,
        expected: number,
    ) => {
        const prng = new Prng();
        const pattern = [0, 0, 2, 0, 3, 0, 5, 0, 0, 0, 7, 9, 11, 13, 0, 0];
        const arrays = new Array(1000)
            .fill(pattern)
            .map(array => array.map((e: number) => e + prng.next_integer(0, 10)));
        test_with_bytes(compress_fn, decompress_fn, arrays.flat(), expected);
    },
);

test.each(
    prepare_test_data(
        [
            [prs_js[0], 0, prs_js[1], prs_js[2], [], 3],
            [prs_js[0], 1, prs_js[1], prs_js[2], [111], 4],
            [prs_js[0], 2, prs_js[1], prs_js[2], [111, 224], 5],
            [prs_js[0], 3, prs_js[1], prs_js[2], [56, 237, 158], 6],
        ],
        [
            [prs_wasm[0], 0, prs_wasm[1], prs_wasm[2], [], 3],
            [prs_wasm[0], 1, prs_wasm[1], prs_wasm[2], [111], 4],
            [prs_wasm[0], 2, prs_wasm[1], prs_wasm[2], [111, 224], 5],
            [prs_wasm[0], 3, prs_wasm[1], prs_wasm[2], [56, 237, 158], 6],
        ],
    ),
)(
    "%s PRS compression and decompression, %d bytes",
    (
        _name: string,
        _num_bytes: number,
        compress_fn: CompressionFunction,
        decompress_fn: CompressionFunction,
        bytes: number[],
        expected: number,
    ) => {
        test_with_bytes(compress_fn, decompress_fn, bytes, expected);
    },
);

function test_with_quest(
    compress_fn: CompressionFunction,
    decompress_fn: CompressionFunction,
    quest_path: string,
): void {
    const buffer = readFileSync(quest_path);
    const orig = compress_fn(new BufferCursor(buffer, Endianness.Little));
    const test = decompress_fn(prs_compress_js(orig));
    orig.seek_start(0);

    expect(test.size).toBe(orig.size);

    let matching_bytes = 0;

    while (orig.bytes_left) {
        const test_byte = test.u8();
        const orig_byte = orig.u8();

        if (test_byte !== orig_byte) {
            throw new Error(
                `Byte ${matching_bytes} didn't match, expected ${orig_byte}, got ${test_byte}.`,
            );
        }

        matching_bytes++;
    }

    expect(matching_bytes).toBe(orig.size);
}

test.each(prepare_test_data([prs_js], [prs_wasm]))(
    "%s PRS compression and decompression of quest118_e.bin",
    (_name: string, compress_fn: CompressionFunction, decompress_fn: CompressionFunction) => {
        test_with_quest(compress_fn, decompress_fn, "test/resources/quest118_e.bin");
    },
);

class Prng {
    seed = 1;

    next(): number {
        const x = Math.sin(this.seed++) * 10000;
        return x - Math.floor(x);
    }

    next_integer(min: number, max: number): number {
        return Math.floor(this.next() * (max + 1 - min)) + min;
    }
}
