import { BufferCursor } from "../BufferCursor";
import { decrypt } from "../encryption/prc";
import { decompress } from "../compression/prs";
import Logger from "js-logger";

const logger = Logger.get("data_formats/parsing/prc");

/**
 * Decrypts and decompresses a .prc file.
 */
export function parse_prc(cursor: BufferCursor): BufferCursor {
    // Unencrypted, decompressed size.
    const size = cursor.u32();
    let key = cursor.u32();
    const out = decompress(decrypt(key, cursor));

    if (out.size !== size) {
        logger.warn(
            `Size of decrypted, decompressed file was ${out.size} instead of expected ${size}.`
        );
    }

    return out;
}
