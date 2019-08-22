import { View } from "./View";
import { el } from "./dom";
import "./ToolBar.css";
import { LabelledControl } from "./LabelledControl";

export class ToolBar extends View {
    readonly element = el("div", { class: "core_ToolBar" });
    readonly height = 33;

    constructor(...children: View[]) {
        super();

        this.element.style.height = `${this.height}px`;

        for (const child of children) {
            if (child instanceof LabelledControl) {
                const group = el("div", { class: "core_ToolBar_group" });

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
