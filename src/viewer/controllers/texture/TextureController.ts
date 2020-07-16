import { Controller } from "../../../core/controllers/Controller";
import { filename_extension } from "../../../core/util";
import { read_file } from "../../../core/files";
import { is_xvm, parse_xvm, XvrTexture } from "../../../core/data_formats/parsing/ninja/texture";
import { ArrayBufferCursor } from "../../../core/data_formats/block/cursor/ArrayBufferCursor";
import { Endianness } from "../../../core/data_formats/block/Endianness";
import { parse_afs } from "../../../core/data_formats/parsing/afs";
import { LogManager } from "../../../core/Logger";
import { WritableListProperty } from "../../../core/observable/property/list/WritableListProperty";
import { list_property, property } from "../../../core/observable";
import { ListProperty } from "../../../core/observable/property/list/ListProperty";
import { prs_decompress } from "../../../core/data_formats/compression/prs/decompress";
import { failure, Result, result_builder } from "../../../core/Result";
import { Severity } from "../../../core/Severity";
import { Property } from "../../../core/observable/property/Property";
import { WritableProperty } from "../../../core/observable/property/WritableProperty";

const logger = LogManager.get("viewer/controllers/TextureController");

export class TextureController extends Controller {
    private readonly _textures: WritableListProperty<XvrTexture> = list_property();
    private readonly _result_dialog_visible = property(false);
    private readonly _result: WritableProperty<Result<unknown> | undefined> = property(undefined);
    private readonly _result_problems_message = property("");
    private readonly _result_error_message = property("");

    readonly textures: ListProperty<XvrTexture> = this._textures;
    readonly result_dialog_visible: Property<boolean> = this._result_dialog_visible;
    readonly result: Property<Result<unknown> | undefined> = this._result;
    readonly result_problems_message: Property<string> = this._result_problems_message;
    readonly result_error_message: Property<string> = this._result_error_message;

    load_file = async (file: File): Promise<void> => {
        this._result_problems_message.val = `Encountered some problems while opening "${file.name}".`;
        this._result_error_message.val = `Couldn't open "${file.name}".`;

        try {
            const ext = filename_extension(file.name).toLowerCase();
            const buffer = await read_file(file);
            const cursor = new ArrayBufferCursor(buffer, Endianness.Little);

            if (ext === "xvm") {
                const xvm_result = parse_xvm(cursor);
                this.set_result(xvm_result);

                if (xvm_result.success) {
                    this._textures.val = xvm_result.value.textures;
                }
            } else if (ext === "afs") {
                const rb = result_builder(logger);
                const afs_result = parse_afs(cursor);
                rb.add_result(afs_result);

                if (!afs_result.success) {
                    this.set_result(rb.failure());
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
                        this.set_result(rb.success(textures));
                    } else {
                        this.set_result(rb.failure());
                    }

                    this._textures.val = textures;
                }
            } else {
                logger.debug(`Unsupported file extension in filename "${file.name}".`);
                this.set_result(
                    failure([{ severity: Severity.Error, ui_message: "Unsupported file type." }]),
                );
            }
        } catch (e) {
            logger.error("Couldn't read file.", e);
            this.set_result(failure());
        }
    };

    dismiss_result_dialog = (): void => {
        this._result_dialog_visible.val = false;
    };

    private set_result(result: Result<unknown>): void {
        this._result.val = result;

        if (result.problems.length) {
            this._result_dialog_visible.val = true;
        }
    }
}
