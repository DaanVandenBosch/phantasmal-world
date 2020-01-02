import { list_property } from "../../core/observable";
import { parse_xvm } from "../../core/data_formats/parsing/ninja/texture";
import { read_file } from "../../core/read_file";
import { ArrayBufferCursor } from "../../core/data_formats/cursor/ArrayBufferCursor";
import { Endianness } from "../../core/data_formats/Endianness";
import { Store } from "../../core/stores/Store";
import { LogManager } from "../../core/Logger";
import { WritableListProperty } from "../../core/observable/property/list/WritableListProperty";
import { Texture } from "three";
import { ListProperty } from "../../core/observable/property/list/ListProperty";
import { filename_extension } from "../../core/util";
import { xvr_texture_to_texture } from "../../core/rendering/conversion/ninja_textures";
import { parse_afs } from "../../core/data_formats/parsing/afs";

const logger = LogManager.get("viewer/stores/TextureStore");

export type TextureWithSize = {
    readonly texture?: Texture;
    readonly width: number;
    readonly height: number;
};

export class TextureStore extends Store {
    private readonly _textures: WritableListProperty<TextureWithSize> = list_property();
    readonly textures: ListProperty<TextureWithSize> = this._textures;

    load_file = async (file: File): Promise<void> => {
        try {
            const ext = filename_extension(file.name).toLowerCase();
            const buffer = await read_file(file);

            if (ext === "xvm") {
                const xvm = parse_xvm(new ArrayBufferCursor(buffer, Endianness.Little));

                this._textures.splice(
                    0,
                    Infinity,
                    ...xvm.textures.map(tex => {
                        let texture: Texture | undefined = undefined;

                        try {
                            texture = xvr_texture_to_texture(tex);
                        } catch (e) {
                            logger.error("Couldn't convert XVR texture.", e);
                        }

                        return {
                            texture,
                            width: tex.width,
                            height: tex.height,
                        };
                    }),
                );
            } else if (ext === "afs") {
                const afs = parse_afs(new ArrayBufferCursor(buffer, Endianness.Little));
                const textures: TextureWithSize[] = [];

                for (const buffer of afs) {
                    const xvm = parse_xvm(new ArrayBufferCursor(buffer, Endianness.Little));

                    for (const tex of xvm.textures) {
                        let texture: Texture | undefined = undefined;

                        try {
                            texture = xvr_texture_to_texture(tex);
                        } catch (e) {
                            logger.error("Couldn't convert XVR texture.", e);
                        }

                        textures.push({
                            texture,
                            width: tex.width,
                            height: tex.height,
                        });
                    }
                }

                this._textures.splice(0, Infinity, ...textures);
            }
        } catch (e) {
            logger.error("Couldn't read file.", e);
        }
    };
}
