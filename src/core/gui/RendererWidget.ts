import { ResizableWidget } from "./ResizableWidget";
import { el } from "./dom";
import { Renderer } from "../rendering/Renderer";

export class RendererWidget extends ResizableWidget {
    readonly element = el.div({ class: "core_RendererWidget" });

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
