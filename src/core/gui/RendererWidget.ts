import { ResizableWidget } from "./ResizableWidget";
import { create_element } from "./dom";
import { Renderer } from "../rendering/Renderer";

export class RendererWidget extends ResizableWidget {
    constructor(private renderer: Renderer) {
        super(create_element("div"));

        this.element.append(renderer.dom_element);

        this.disposable(renderer);
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
