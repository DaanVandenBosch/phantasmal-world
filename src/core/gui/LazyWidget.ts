import { Widget } from "./Widget";
import { Resizable } from "./Resizable";
import { ResizableWidget } from "./ResizableWidget";
import { div } from "./dom";

export class LazyWidget extends ResizableWidget {
    readonly element = div({ className: "core_LazyView" });

    private initialized = false;
    private view: (Widget & Resizable) | undefined;

    constructor(private create_view: () => Promise<Widget & Resizable>) {
        super();

        this.visible.val = false;
    }

    protected set_visible(visible: boolean): void {
        super.set_visible(visible);

        if (visible && !this.initialized) {
            this.initialized = true;

            this.create_view().then(view => {
                if (this.disposed) {
                    view.dispose();
                } else {
                    this.view = this.disposable(view);
                    this.view.resize(this.width, this.height);
                    this.element.append(view.element);
                }
            });
        }

        this.finalize_construction();
    }

    resize(width: number, height: number): this {
        super.resize(width, height);

        if (this.view) {
            this.view.resize(width, height);
        }

        return this;
    }
}
