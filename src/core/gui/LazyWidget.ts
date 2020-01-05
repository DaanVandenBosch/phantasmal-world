import { ResizableWidget } from "./ResizableWidget";
import { div } from "./dom";
import { Widget } from "./Widget";
import { Resizable } from "./Resizable";

export class LazyWidget extends ResizableWidget {
    private initialized = false;
    private view: (Widget & Resizable) | undefined;

    readonly element = div({ className: "core_LazyView" });

    get children(): readonly (Widget & Resizable)[] {
        return this.view ? [this.view] : [];
    }

    constructor(private create_view: () => Promise<Widget & Resizable>) {
        super();

        this.visible.val = false;
    }

    resize(width: number, height: number): this {
        super.resize(width, height);

        if (this.view) {
            this.view.resize(width, height);
        }

        return this;
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
                    this.view.activate();
                }
            });
        }

        this.finalize_construction();
    }
}
