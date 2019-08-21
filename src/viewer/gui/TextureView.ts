import { create_el } from "../../core/gui/dom";
import { ResizableView } from "../../core/gui/ResizableView";

export class TextureView extends ResizableView {
    element = create_el("div", "viewer_TextureView", el => (el.textContent = "Texture"));
}
