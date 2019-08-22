import { el } from "./dom";
import { WritableProperty } from "../observable/WritableProperty";
import { property } from "../observable";
import { LabelledControl } from "./LabelledControl";

export class CheckBox extends LabelledControl {
    readonly element: HTMLInputElement = el("input", { class: "core_CheckBox" });

    readonly checked: WritableProperty<boolean> = property(false);

    readonly preferred_label_position = "right";

    constructor(checked: boolean = false, label?: string) {
        super(label);

        this.element.type = "checkbox";
        this.element.onchange = () => (this.checked.val = this.element.checked);

        this.disposables(
            this.checked.observe(checked => (this.element.checked = checked)),

            this.enabled.observe(enabled => (this.element.disabled = !enabled)),
        );

        this.checked.val = checked;
    }
}
