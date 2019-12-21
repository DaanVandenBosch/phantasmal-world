import { TabContainer } from "../../core/gui/TabContainer";
import { Model3DView } from "./model_3d/Model3DView";
import { TextureView } from "./TextureView";

export class ViewerView extends TabContainer {
    constructor(
        create_model_3d_view: () => Promise<Model3DView>,
        create_texture_view: () => Promise<TextureView>,
    ) {
        super({
            class: "viewer_ViewerView",
            tabs: [
                {
                    title: "Models",
                    key: "model",
                    create_view: create_model_3d_view,
                },
                {
                    title: "Textures",
                    key: "texture",
                    create_view: create_texture_view,
                },
            ],
        });

        this.finalize_construction();
    }
}
