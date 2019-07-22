import Logger from "js-logger";
import { prs_decompress } from "../compression/prs/decompress";
import { Cursor } from "../cursor/Cursor";
import { decrypt } from "../encryption/prc";

const logger = Logger.get("data_formats/parsing/prc");

/**
 * Decrypts and decompresses a .prc file.
 */
export function parse_prc(cursor: Cursor): Cursor {
    // Unencrypted, decompressed size.
    const size = cursor.u32();
    let key = cursor.u32();
    const out = prs_decompress(decrypt(key, cursor));

    if (out.size !== size) {
        logger.warn(
            `Size of decrypted, decompressed file was ${out.size} instead of expected ${size}.`
        );
    }

    return out;
}
