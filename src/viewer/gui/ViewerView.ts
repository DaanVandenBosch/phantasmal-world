import { TabContainer } from "../../core/gui/TabContainer";

export class ViewerView extends TabContainer {
    constructor() {
        super(
            {
                title: "Models",
                key: "model",
                create_view: async function() {
                    return new (await import("./model_3d/Model3DView")).Model3DView();
                },
            },
            {
                title: "Textures",
                key: "texture",
                create_view: async function() {
                    return new (await import("./TextureView")).TextureView();
                },
            },
        );
    }
}
