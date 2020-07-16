/**
 * @file Exposes the WASM PRS functions depending on whether the WASM module is available or not.
 */

import { browser_supports_webassembly } from "../../../util";
import { Cursor } from "../../block/cursor/Cursor";
import { ArrayBufferCursor } from "../../block/cursor/ArrayBufferCursor";
import { Endianness } from "../../block/Endianness";

type PrsRsModule = typeof import("prs-rs");

class PrsWasm {
    constructor(private module: PrsRsModule) {}

    public prs_compress_wasm(cursor: Cursor): Cursor {
        const bytes = new Uint8Array(cursor.array_buffer());
        const result = this.module.compress(bytes);
        return new ArrayBufferCursor(
            result.buffer,
            Endianness.Little,
            result.byteOffset,
            result.length,
        );
    }

    public prs_decompress_wasm(cursor: Cursor): Cursor {
        const bytes = new Uint8Array(cursor.array_buffer());
        const result = this.module.decompress(bytes);
        return new ArrayBufferCursor(
            result.buffer,
            Endianness.Little,
            result.byteOffset,
            result.length,
        );
    }
}

// Load prs-rs if it exists.
let prs_wasm: PrsWasm | undefined = undefined;
try {
    if (browser_supports_webassembly()) {
        prs_wasm = new PrsWasm(require("prs-rs"));
    }
} catch (e) {
    // Webpack will emit a warning if module is missing.
}

export function get_prs_wasm_module(): PrsWasm | undefined {
    return prs_wasm;
}
