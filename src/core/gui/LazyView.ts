import { View } from "./View";
import { create_element } from "./dom";
import { Resizable } from "./Resizable";
import { ResizableView } from "./ResizableView";

export class LazyView extends ResizableView {
    readonly element = create_element("div", { class: "core_LazyView" });

    private initialized = false;
    private view: View & Resizable | undefined;

    constructor(private create_view: () => Promise<View & Resizable>) {
        super();

        this.visible.val = false;

        this.disposables(
            this.visible.observe(visible => {
                if (visible && !this.initialized) {
                    this.initialized = true;

                    this.create_view().then(view => {
                        this.view = this.disposable(view);
                        this.view.resize(this.width, this.height);
                        this.element.append(view.element);
                    });
                }
            }),
        );
    }

    resize(width: number, height: number): this {
        super.resize(width, height);

        if (this.view) {
            this.view.resize(width, height);
        }

        return this;
    }
}
