import { ResizableView } from "./ResizableView";
import { create_el } from "./dom";
import { Renderer } from "../../../core/rendering/Renderer";

export class RendererView extends ResizableView {
    readonly element = create_el("div");

    constructor(private renderer: Renderer) {
        super();

        this.element.append(renderer.dom_element);

        this.disposable(renderer);

        // TODO: stop on hidden
        renderer.start_rendering();
    }

    resize(width: number, height: number): this {
        super.resize(width, height);

        this.renderer.set_size(width, height);

        return this;
    }
}
