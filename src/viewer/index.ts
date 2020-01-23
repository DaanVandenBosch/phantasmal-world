import { ViewerView } from "./gui/ViewerView";
import { GuiStore } from "../core/stores/GuiStore";
import { HttpClient } from "../core/HttpClient";
import { DisposableThreeRenderer } from "../core/rendering/ThreeRenderer";
import { Disposable } from "../core/observable/Disposable";
import { Disposer } from "../core/observable/Disposer";
import { Random } from "../core/Random";
import { Renderer } from "../core/rendering/Renderer";

export function initialize_viewer(
    http_client: HttpClient,
    random: Random,
    gui_store: GuiStore,
    create_three_renderer: () => DisposableThreeRenderer,
): { view: ViewerView } & Disposable {
    const disposer = new Disposer();

    const view = new ViewerView(
        gui_store,

        async () => {
            const { ModelController } = await import("./controllers/model/ModelController");
            const { ModelRenderer } = await import("./rendering/ModelRenderer");
            const { ModelView } = await import("./gui/model/ModelView");
            const { CharacterClassAssetLoader } = await import(
                "./loading/CharacterClassAssetLoader"
            );
            const { ModelToolBarView } = await import("./gui/model/ModelToolBarView");
            const { ModelStore } = await import("./stores/ModelStore");
            const { ModelToolBarController } = await import(
                "./controllers/model/ModelToolBarController"
            );
            const { CharacterClassOptionsView } = await import(
                "./gui/model/CharacterClassOptionsView"
            );
            const { CharacterClassOptionsController } = await import(
                "./controllers/model/CharacterClassOptionsController"
            );

            const asset_loader = disposer.add(new CharacterClassAssetLoader(http_client));
            const store = disposer.add(new ModelStore(gui_store, asset_loader, random));
            const model_controller = new ModelController(store);
            const model_tool_bar_controller = new ModelToolBarController(store);
            const character_class_options_controller = new CharacterClassOptionsController(store);

            return new ModelView(
                model_controller,
                new ModelToolBarView(model_tool_bar_controller),
                new CharacterClassOptionsView(character_class_options_controller),
                new ModelRenderer(store, create_three_renderer()),
            );
        },

        async () => {
            const { TextureController } = await import("./controllers/TextureController");
            const { TextureView } = await import("./gui/TextureView");

            const controller = disposer.add(new TextureController());

            let renderer: Renderer;

            if (gui_store.feature_active("webgpu")) {
                const { TextureWebgpuRenderer } = await import("./rendering/TextureWebgpuRenderer");
                renderer = new TextureWebgpuRenderer(controller);
            } else if (gui_store.feature_active("webgl")) {
                const { TextureWebglRenderer } = await import("./rendering/TextureWebglRenderer");
                renderer = new TextureWebglRenderer(controller);
            } else {
                const { TextureRenderer } = await import("./rendering/TextureRenderer");
                renderer = new TextureRenderer(controller, create_three_renderer());
            }

            return new TextureView(controller, renderer);
        },
    );

    return {
        view,
        dispose(): void {
            disposer.dispose();
        },
    };
}
