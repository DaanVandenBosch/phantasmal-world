import { Widget } from "./Widget";
import { create_element } from "./dom";
import "./ToolBar.css";
import { LabelledControl } from "./LabelledControl";

export class ToolBar extends Widget {
    readonly element = create_element("div", { class: "core_ToolBar" });

    readonly height = 33;

    constructor(...children: Widget[]) {
        super();

        this.element.style.height = `${this.height}px`;

        for (const child of children) {
            if (child instanceof LabelledControl) {
                const group = create_element("div", { class: "core_ToolBar_group" });

                if (
                    child.preferred_label_position === "left" ||
                    child.preferred_label_position === "top"
                ) {
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
