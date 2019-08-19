import { View } from "./View";
import { create_el } from "./dom";
import "./ToolBar.css";

export class ToolBar extends View {
    readonly element = create_el("div", "core_ToolBar");
    readonly height = 32;

    constructor(...children: View[]) {
        super();

        this.element.style.height = `${this.height}px`;

        for (const child of children) {
            this.element.append(child.element);
            this.disposable(child);
        }
    }
}
