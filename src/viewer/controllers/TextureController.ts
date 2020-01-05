import { Controller } from "../../core/controllers/Controller";
import { filename_extension } from "../../core/util";
import { read_file } from "../../core/files";
import { parse_xvm, XvrTexture } from "../../core/data_formats/parsing/ninja/texture";
import { ArrayBufferCursor } from "../../core/data_formats/cursor/ArrayBufferCursor";
import { Endianness } from "../../core/data_formats/Endianness";
import { parse_afs } from "../../core/data_formats/parsing/afs";
import { LogManager } from "../../core/Logger";
import { WritableListProperty } from "../../core/observable/property/list/WritableListProperty";
import { list_property } from "../../core/observable";
import { ListProperty } from "../../core/observable/property/list/ListProperty";

const logger = LogManager.get("viewer/controllers/TextureController");

export class TextureController extends Controller {
    private readonly _textures: WritableListProperty<XvrTexture> = list_property();
    readonly textures: ListProperty<XvrTexture> = this._textures;

    load_file = async (file: File): Promise<void> => {
        try {
            const ext = filename_extension(file.name).toLowerCase();
            const buffer = await read_file(file);

            if (ext === "xvm") {
                const xvm = parse_xvm(new ArrayBufferCursor(buffer, Endianness.Little));

                this._textures.splice(0, Infinity, ...xvm.textures);
            } else if (ext === "afs") {
                const afs = parse_afs(new ArrayBufferCursor(buffer, Endianness.Little));
                const textures: XvrTexture[] = [];

                for (const buffer of afs) {
                    const xvm = parse_xvm(new ArrayBufferCursor(buffer, Endianness.Little));
                    textures.push(...xvm.textures);
                }

                this._textures.val = textures;
            }
        } catch (e) {
            logger.error("Couldn't read file.", e);
        }
    };
}
