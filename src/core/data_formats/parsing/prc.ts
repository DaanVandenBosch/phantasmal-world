import { prs_decompress } from "../compression/prs/decompress";
import { Cursor } from "../block/cursor/Cursor";
import { prc_decrypt } from "../encryption/prc";
import { LogManager } from "../../logging";
import { Result, ResultBuilder } from "../../Result";
import { Severity } from "../../Severity";

const logger = LogManager.get("core/data_formats/parsing/prc");

/**
 * Decrypts and decompresses a .prc file.
 */
export function parse_prc(cursor: Cursor): Result<Cursor> {
    const rb = new ResultBuilder<Cursor>(logger);
    // Unencrypted, decompressed size.
    const size = cursor.u32();
    const key = cursor.u32();
    const out = prs_decompress(prc_decrypt(key, cursor));
    rb.add_result(out);

    if (!out.success) {
        return rb.failure();
    }

    if (out.value.size !== size) {
        rb.add_problem(
            Severity.Warning,
            `Size of decrypted, decompressed file was ${out.value.size} instead of expected ${size}.`,
        );
    }

    return rb.success(out.value);
}
