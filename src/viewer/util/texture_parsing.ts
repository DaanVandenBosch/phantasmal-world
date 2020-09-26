import { Cursor } from "../../core/data_formats/block/cursor/Cursor";
import { Result, ResultBuilder, success } from "../../core/Result";
import { is_xvm, parse_xvm, XvrTexture } from "../../core/data_formats/parsing/ninja/texture";
import { parse_afs } from "../../core/data_formats/parsing/afs";
import { Severity } from "../../core/Severity";
import { ArrayBufferCursor } from "../../core/data_formats/block/cursor/ArrayBufferCursor";
import { Endianness } from "../../core/data_formats/block/Endianness";
import { prs_decompress } from "../../core/data_formats/compression/prs/decompress";
import { LogManager } from "../../core/logging";

const logger = LogManager.get("viewer/util/texture_parsing");

export function parse_xvm_textures(cursor: Cursor): Result<XvrTexture[]> {
    const xvm_result = parse_xvm(cursor);

    if (!xvm_result.success) {
        return xvm_result;
    }

    return success(xvm_result.value.textures);
}

export function parse_afs_textures(cursor: Cursor): Result<XvrTexture[]> {
    const rb = new ResultBuilder<XvrTexture[]>(logger);
    const afs_result = parse_afs(cursor);
    rb.add_result(afs_result);

    if (!afs_result.success) {
        return rb.failure();
    }

    if (afs_result.value.length === 0) {
        rb.add_problem(Severity.Info, "AFS archive contains no files.");
    }

    const textures: XvrTexture[] = afs_result.value.flatMap(file => {
        const cursor = new ArrayBufferCursor(file, Endianness.Little);

        if (is_xvm(cursor)) {
            const xvm_result = parse_xvm(cursor);
            rb.add_result(xvm_result);
            return xvm_result.value?.textures ?? [];
        } else {
            const decompression_result = prs_decompress(cursor.seek_start(0));
            rb.add_result(decompression_result);

            if (!decompression_result.success) {
                return [];
            }

            const xvm_result = parse_xvm(decompression_result.value);
            rb.add_result(xvm_result);
            return xvm_result.value?.textures ?? [];
        }
    });

    return rb.success(textures);
}
