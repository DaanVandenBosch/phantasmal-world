import { ViewerView } from "./gui/ViewerView";
import { GuiStore } from "../core/stores/GuiStore";

export function initialize_viewer(gui_store: GuiStore): ViewerView {
    return new ViewerView(
        async () => {
            const { Model3DStore } = await import("./stores/Model3DStore");
            const { Model3DView } = await import("./gui/model_3d/Model3DView");
            return new Model3DView(gui_store, new Model3DStore());
        },

        async () => {
            const { TextureStore } = await import("./stores/TextureStore");
            const { TextureView } = await import("./gui/TextureView");
            return new TextureView(gui_store, new TextureStore());
        },
    );
}
