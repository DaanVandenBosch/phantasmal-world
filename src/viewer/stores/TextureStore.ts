import { property } from "../../core/observable";
import { parse_xvm, Xvm } from "../../core/data_formats/parsing/ninja/texture";
import { Property } from "../../core/observable/property/Property";
import { read_file } from "../../core/read_file";
import { ArrayBufferCursor } from "../../core/data_formats/cursor/ArrayBufferCursor";
import { Endianness } from "../../core/data_formats/Endianness";
import Logger = require("js-logger");

const logger = Logger.get("viewer/stores/TextureStore");

export class TextureStore {
    private readonly _current_xvm = property<Xvm | undefined>(undefined);
    readonly current_xvm: Property<Xvm | undefined> = this._current_xvm;

    load_file = async (file: File): Promise<void> => {
        try {
            const buffer = await read_file(file);
            this._current_xvm.val = parse_xvm(new ArrayBufferCursor(buffer, Endianness.Little));
        } catch (e) {
            logger.error("Couldn't read file.", e);
        }
    };
}

export const texture_store = new TextureStore();
