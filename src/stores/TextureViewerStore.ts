import { observable } from "mobx";
import { Xvm, parse_xvm } from "../data_formats/parsing/ninja/texture";
import { ArrayBufferCursor } from "../data_formats/cursor/ArrayBufferCursor";
import { read_file } from "../read_file";
import { Endianness } from "../data_formats/Endianness";
import Logger from "js-logger";

const logger = Logger.get("stores/TextureViewerStore");

class TextureViewStore {
    @observable.ref current_xvm?: Xvm;

    // TODO: notify user of problems.
    load_file = async (file: File) => {
        try {
            const buffer = await read_file(file);
            this.current_xvm = parse_xvm(new ArrayBufferCursor(buffer, Endianness.Little));
        } catch (e) {
            logger.error("Couldn't read file.", e);
        }
    };
}

export const texture_viewer_store = new TextureViewStore();
