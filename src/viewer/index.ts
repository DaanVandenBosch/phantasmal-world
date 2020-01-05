import { ViewerView } from "./gui/ViewerView";
import { GuiStore } from "../core/stores/GuiStore";
import { HttpClient } from "../core/HttpClient";
import { DisposableThreeRenderer } from "../core/rendering/Renderer";
import { Disposable } from "../core/observable/Disposable";
import { Disposer } from "../core/observable/Disposer";
import { TextureRenderer } from "./rendering/TextureRenderer";
import { ModelRenderer } from "./rendering/ModelRenderer";
import { Random } from "../core/Random";
import { ModelToolBarView } from "./gui/model/ModelToolBarView";
import { ModelStore } from "./stores/ModelStore";
import { ModelToolBarController } from "./controllers/model/ModelToolBarController";
import { CharacterClassOptionsView } from "./gui/model/CharacterClassOptionsView";
import { CharacterClassOptionsController } from "./controllers/model/CharacterClassOptionsController";

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
            const { ModelView } = await import("./gui/model/ModelView");
            const { CharacterClassAssetLoader } = await import(
                "./loading/CharacterClassAssetLoader"
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

            return new TextureView(
                controller,
                new TextureRenderer(controller, create_three_renderer()),
            );
        },
    );

    return {
        view,
        dispose(): void {
            disposer.dispose();
        },
    };
}
