import { Controller } from "../../../core/controllers/Controller";
import { filename_extension } from "../../../core/util";
import { read_file } from "../../../core/files";
import { XvrTexture } from "../../../core/data_formats/parsing/ninja/texture";
import { ArrayBufferCursor } from "../../../core/data_formats/block/cursor/ArrayBufferCursor";
import { Endianness } from "../../../core/data_formats/block/Endianness";
import { LogManager } from "../../../core/Logger";
import { WritableListProperty } from "../../../core/observable/property/list/WritableListProperty";
import { list_property, property } from "../../../core/observable";
import { ListProperty } from "../../../core/observable/property/list/ListProperty";
import { failure, problem, Result } from "../../../core/Result";
import { Severity } from "../../../core/Severity";
import { Property } from "../../../core/observable/property/Property";
import { WritableProperty } from "../../../core/observable/property/WritableProperty";
import { parse_afs_textures, parse_xvm_textures } from "../../util/texture_parsing";

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
            let result: Result<XvrTexture[]>;

            if (ext === "xvm") {
                result = parse_xvm_textures(cursor);
            } else if (ext === "afs") {
                result = parse_afs_textures(cursor);
            } else {
                logger.debug(`Unsupported file extension in filename "${file.name}".`);
                result = failure(problem(Severity.Error, "Unsupported file type."));
            }

            this.set_result(result);

            if (result.success) {
                this._textures.val = result.value;
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
