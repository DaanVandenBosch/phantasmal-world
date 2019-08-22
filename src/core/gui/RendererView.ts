import { ResizableView } from "./ResizableView";
import { el } from "./dom";
import { Renderer } from "../rendering/Renderer";

export class RendererView extends ResizableView {
    readonly element = el("div");

    constructor(private renderer: Renderer) {
        super();

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
