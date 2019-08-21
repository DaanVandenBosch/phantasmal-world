import { View } from "./View";
import { create_el } from "./dom";
import "./ToolBar.css";
import { LabelledControl } from "./LabelledControl";

export class ToolBar extends View {
    readonly element = create_el("div", "core_ToolBar");
    readonly height = 35;

    constructor(...children: View[]) {
        super();

        this.element.style.height = `${this.height}px`;

        for (const child of children) {
            if (child instanceof LabelledControl) {
                const group = create_el("div", "core_ToolBar_group");

                if (child.preferred_label_position === "left") {
                    group.append(child.label.element, child.element);
                } else {
                    group.append(child.element, child.label.element);
                }

                this.element.append(group);
            } else {
                this.element.append(child.element);
                this.disposable(child);
            }
        }
    }
}
