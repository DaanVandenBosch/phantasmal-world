import { ResizableWidget } from "./ResizableWidget";
import { Renderer } from "../rendering/Renderer";
import { div } from "./dom";
import { Widget } from "./Widget";

export class RendererWidget extends ResizableWidget {
    readonly element = div({ className: "core_RendererWidget" });
    readonly children: readonly Widget[] = [];

    constructor(private renderer: Renderer) {
        super();

        this.element.append(renderer.canvas_element);

        this.disposable(renderer);

        this.finalize_construction();
    }

    start_rendering(): void {
        this.renderer.start_rendering();
    }

    stop_rendering(): void {
        this.renderer.stop_rendering();
    }

    resize(width: number, height: number): this {
        super.resize(width, height);

        this.renderer.set_size(width, height);

        return this;
    }
}
