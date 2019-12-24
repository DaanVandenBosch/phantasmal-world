import { ViewerView } from "./gui/ViewerView";
import { GuiStore } from "../core/stores/GuiStore";
import { HttpClient } from "../core/HttpClient";
import { DisposableThreeRenderer } from "../core/rendering/Renderer";
import { Disposable } from "../core/observable/Disposable";
import { Disposer } from "../core/observable/Disposer";

export function initialize_viewer(
    http_client: HttpClient,
    gui_store: GuiStore,
    create_three_renderer: () => DisposableThreeRenderer,
): { view: ViewerView } & Disposable {
    const disposer = new Disposer();

    const view = new ViewerView(
        async () => {
            const { Model3DStore } = await import("./stores/Model3DStore");
            const { Model3DView } = await import("./gui/model_3d/Model3DView");
            const { CharacterClassAssetLoader } = await import(
                "./loading/CharacterClassAssetLoader"
            );
            const store = new Model3DStore(new CharacterClassAssetLoader(http_client));

            if (disposer.disposed) {
                store.dispose();
            } else {
                disposer.add(store);
            }

            return new Model3DView(gui_store, store, create_three_renderer());
        },

        async () => {
            const { TextureStore } = await import("./stores/TextureStore");
            const { TextureView } = await import("./gui/TextureView");
            const store = new TextureStore();

            if (disposer.disposed) {
                store.dispose();
            } else {
                disposer.add(store);
            }

            return new TextureView(gui_store, store, create_three_renderer());
        },
    );

    return {
        view,
        dispose(): void {
            disposer.dispose();
        },
    };
}
