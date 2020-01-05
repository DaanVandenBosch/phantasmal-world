import { Widget, WidgetOptions } from "./Widget";
import "./ToolBar.css";
import { LabelledControl } from "./LabelledControl";
import { div } from "./dom";

export class ToolBar extends Widget {
    readonly element = div({ className: "core_ToolBar" });
    readonly height = 33;
    readonly children: readonly Widget[];

    constructor(options?: WidgetOptions, ...children: Widget[]) {
        // noinspection SuspiciousTypeOfGuard
        super(options instanceof Widget ? undefined : options);

        this.element.style.height = `${this.height}px`;
        // noinspection SuspiciousTypeOfGuard
        this.children = options instanceof Widget ? [options, ...children] : children;

        for (const child of this.children) {
            this.disposable(child);

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
