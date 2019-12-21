import { ViewerView } from "./gui/ViewerView";
import { GuiStore } from "../core/stores/GuiStore";
import { HttpClient } from "../core/HttpClient";

export function initialize_viewer(http_client: HttpClient, gui_store: GuiStore): ViewerView {
    return new ViewerView(
        async () => {
            const { Model3DStore } = await import("./stores/Model3DStore");
            const { Model3DView } = await import("./gui/model_3d/Model3DView");
            const { CharacterClassAssetLoader } = await import(
                "./loading/CharacterClassAssetLoader"
            );
            return new Model3DView(
                gui_store,
                new Model3DStore(new CharacterClassAssetLoader(http_client)),
            );
        },

        async () => {
            const { TextureStore } = await import("./stores/TextureStore");
            const { TextureView } = await import("./gui/TextureView");
            return new TextureView(gui_store, new TextureStore());
        },
    );
}
