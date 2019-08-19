import { TabContainer } from "../../core/gui/TabContainer";
import { ResizableView } from "../../core/gui/ResizableView";

export class ViewerView extends ResizableView {
    private tabs = this.disposable(
        new TabContainer(
            {
                title: "Models",
                key: "model",
                create_view: async () => new (await import("./ModelView")).ModelView(),
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
