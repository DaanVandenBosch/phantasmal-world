import { Widget } from "./Widget";
import { el } from "./dom";
import { Resizable } from "./Resizable";
import { ResizableWidget } from "./ResizableWidget";

export class LazyWidget extends ResizableWidget {
    private initialized = false;
    private view: Widget & Resizable | undefined;

    constructor(private create_view: () => Promise<Widget & Resizable>) {
        super(el.div({ class: "core_LazyView" }));

        this.visible.val = false;
    }

    protected set_visible(visible: boolean): void {
        super.set_visible(visible);

        if (visible && !this.initialized) {
            this.initialized = true;

            this.create_view().then(view => {
                if (!this.disposed) {
                    this.view = this.disposable(view);
                    this.view.resize(this.width, this.height);
                    this.element.append(view.element);
                }
            });
        }

        this.finalize_construction(LazyWidget.prototype);
    }

    resize(width: number, height: number): this {
        super.resize(width, height);

        if (this.view) {
            this.view.resize(width, height);
        }

        return this;
    }
}
