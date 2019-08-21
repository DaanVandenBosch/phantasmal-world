import { create_el } from "./dom";
import { WritableProperty } from "../observable/WritableProperty";
import { property } from "../observable";
import { LabelledControl } from "./LabelledControl";

export class CheckBox extends LabelledControl {
    readonly element: HTMLInputElement = create_el("input", "core_CheckBox");

    readonly checked: WritableProperty<boolean> = property(false);

    readonly preferred_label_position = "right";

    constructor(checked: boolean = false, label?: string) {
        super(label);

        this.element.type = "checkbox";
        this.element.onchange = () => this.checked.set(this.element.checked);

        this.disposables(
            this.checked.observe(checked => (this.element.checked = checked)),

            this.enabled.observe(enabled => (this.element.disabled = !enabled)),
        );

        this.checked.set(checked);
    }
}
