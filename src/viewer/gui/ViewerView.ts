import { TabContainer } from "../../core/gui/TabContainer";
import { Model3DView } from "./model_3d/Model3DView";
import { TextureView } from "./TextureView";
import { ResizableView } from "../../core/gui/ResizableView";
import { GuiStore } from "../../core/stores/GuiStore";

export class ViewerView extends ResizableView {
    private readonly tab_container: TabContainer;

    get element(): HTMLElement {
        return this.tab_container.element;
    }

    constructor(
        gui_store: GuiStore,
        create_model_3d_view: () => Promise<Model3DView>,
        create_texture_view: () => Promise<TextureView>,
    ) {
        super();

        this.tab_container = this.add(
            new TabContainer(gui_store, {
                class: "viewer_ViewerView",
                tabs: [
                    {
                        title: "Models",
                        key: "models",
                        path: "/models",
                        create_view: create_model_3d_view,
                    },
                    {
                        title: "Textures",
                        key: "textures",
                        path: "/textures",
                        create_view: create_texture_view,
                    },
                ],
            }),
        );

        this.finalize_construction();
    }

    resize(width: number, height: number): void {
        this.tab_container.resize(width, height);
    }
}
