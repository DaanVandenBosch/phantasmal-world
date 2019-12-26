import { Widget, WidgetOptions } from "./Widget";
import "./ToolBar.css";
import { LabelledControl } from "./LabelledControl";
import { div } from "./dom";

export type ToolBarOptions = WidgetOptions & {
    children?: Widget[];
};

export class ToolBar extends Widget {
    private readonly children: readonly Widget[];

    readonly element = div({ className: "core_ToolBar" });
    readonly height = 33;

    constructor(options?: ToolBarOptions) {
        super(options);

        this.element.style.height = `${this.height}px`;
        this.children = (options && options.children) || [];

        for (const child of this.children) {
            if (child instanceof LabelledControl && child.label) {
                const group = div({ className: "core_ToolBar_group" });

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

        this.finalize_construction();
    }

    protected set_enabled(enabled: boolean): void {
        super.set_enabled(enabled);

        for (const child of this.children) {
            child.enabled.val = enabled;
        }
    }
}
