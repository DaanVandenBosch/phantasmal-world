import { TabContainer } from "../../core/gui/TabContainer";
import { ResizableWidget } from "../../core/gui/ResizableWidget";

export class ViewerView extends ResizableWidget {
    private tabs = this.disposable(
        new TabContainer(
            {
                title: "Models",
                key: "model",
                create_view: async () => new (await import("./Model3DView")).Model3DView(),
            },
            {
                title: "Textures",
                key: "texture",
                create_view: async () => new (await import("./TextureView")).TextureView(),
            },
        ),
    );

    get element(): HTMLElement {
        return this.tabs.element;
    }

    resize(width: number, height: number): this {
        super.resize(width, height);
        this.tabs.resize(width, height);
        return this;
    }
}
