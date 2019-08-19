import { View } from "./View";
import { create_el } from "./dom";
import { Resizable } from "./Resizable";
import { ResizableView } from "./ResizableView";

export class LazyView extends ResizableView {
    element = create_el("div", "core_LazyView");

    private _visible = false;

    set visible(visible: boolean) {
        if (this._visible !== visible) {
            this._visible = visible;
            this.element.hidden = !visible;

            if (visible && !this.initialized) {
                this.initialized = true;

                this.create_view().then(view => {
                    this.view = this.disposable(view);
                    this.view.resize(this.width, this.height);
                    this.element.append(view.element);
                });
            }
        }
    }

    private initialized = false;
    private view: View & Resizable | undefined;

    constructor(private create_view: () => Promise<View & Resizable>) {
        super();

        this.element.hidden = true;
    }

    resize(width: number, height: number): this {
        super.resize(width, height);

        if (this.view) {
            this.view.resize(width, height);
        }

        return this;
    }
}
