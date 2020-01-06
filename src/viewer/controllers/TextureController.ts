import { Controller } from "../../core/controllers/Controller";
import { filename_extension } from "../../core/util";
import { read_file } from "../../core/files";
import { is_xvm, parse_xvm, XvrTexture } from "../../core/data_formats/parsing/ninja/texture";
import { ArrayBufferCursor } from "../../core/data_formats/cursor/ArrayBufferCursor";
import { Endianness } from "../../core/data_formats/Endianness";
import { parse_afs } from "../../core/data_formats/parsing/afs";
import { LogManager } from "../../core/Logger";
import { WritableListProperty } from "../../core/observable/property/list/WritableListProperty";
import { list_property } from "../../core/observable";
import { ListProperty } from "../../core/observable/property/list/ListProperty";
import { prs_decompress } from "../../core/data_formats/compression/prs/decompress";
import { failure, Result, result_builder } from "../../core/Result";
import { show_result_popup } from "../../core/gui/ResultPopup";
import { Severity } from "../../core/Severity";

const logger = LogManager.get("viewer/controllers/TextureController");

export class TextureController extends Controller {
    private readonly _textures: WritableListProperty<XvrTexture> = list_property();
    readonly textures: ListProperty<XvrTexture> = this._textures;

    load_file = async (file: File): Promise<void> => {
        let result: Result<unknown>;

        try {
            const ext = filename_extension(file.name).toLowerCase();
            const buffer = await read_file(file);
            const cursor = new ArrayBufferCursor(buffer, Endianness.Little);

            if (ext === "xvm") {
                const xvm_result = (result = parse_xvm(cursor));

                if (xvm_result.success) {
                    this._textures.val = xvm_result.value.textures;
                }
            } else if (ext === "afs") {
                const rb = result_builder(logger);
                const afs_result = parse_afs(cursor);
                rb.add_result(afs_result);

                if (!afs_result.success) {
                    result = rb.failure();
                } else {
                    const textures: XvrTexture[] = afs_result.value.flatMap(file => {
                        const cursor = new ArrayBufferCursor(file, Endianness.Little);

                        if (is_xvm(cursor)) {
                            const xvm_result = parse_xvm(cursor);
                            rb.add_result(xvm_result);
                            return xvm_result.value?.textures ?? [];
                        } else {
                            const xvm_result = parse_xvm(prs_decompress(cursor.seek_start(0)));
                            rb.add_result(xvm_result);
                            return xvm_result.value?.textures ?? [];
                        }
                    });

                    if (textures.length) {
                        result = rb.success(textures);
                    } else {
                        result = rb.failure();
                    }

                    this._textures.val = textures;
                }
            } else {
                logger.debug(`Unsupported file extension in filename "${file.name}".`);
                result = failure([
                    { severity: Severity.Error, ui_message: "Unsupported file type." },
                ]);
            }
        } catch (e) {
            logger.error("Couldn't read file.", e);
            result = failure();
        }

        show_result_popup(
            result,
            `Encountered some problems while opening "${file.name}".`,
            `Couldn't open "${file.name}".`,
        );
    };
}
