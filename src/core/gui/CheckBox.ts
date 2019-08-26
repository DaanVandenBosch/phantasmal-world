import { create_element } from "./dom";
import { WritableProperty } from "../observable/WritableProperty";
import { property } from "../observable";
import { LabelledControl } from "./LabelledControl";

export class CheckBox extends LabelledControl {
    readonly element: HTMLInputElement = create_element("input", { class: "core_CheckBox" });

    readonly checked: WritableProperty<boolean> = property(false);

    readonly preferred_label_position = "right";

    constructor(checked: boolean = false, label?: string) {
        super(label);

        this.element.type = "checkbox";
        this.element.onchange = () => (this.checked.val = this.element.checked);

        this.disposables(
            this.checked.observe(({ value }) => (this.element.checked = value)),

            this.enabled.observe(({ value }) => (this.element.disabled = !value)),
        );

        this.checked.val = checked;
    }
}
