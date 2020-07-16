import { prs_decompress } from "../compression/prs/decompress";
import { Cursor } from "../block/cursor/Cursor";
import { prc_decrypt } from "../encryption/prc";
import { LogManager } from "../../Logger";

const logger = LogManager.get("core/data_formats/parsing/prc");

/**
 * Decrypts and decompresses a .prc file.
 */
export function parse_prc(cursor: Cursor): Cursor {
    // Unencrypted, decompressed size.
    const size = cursor.u32();
    const key = cursor.u32();
    const out = prs_decompress(prc_decrypt(key, cursor));

    if (out.size !== size) {
        logger.warn(
            `Size of decrypted, decompressed file was ${out.size} instead of expected ${size}.`,
        );
    }

    return out;
}
