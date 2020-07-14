import { WritableProperty } from "../observable/property/WritableProperty";
import { LabelledControl, LabelledControlOptions } from "./LabelledControl";
import { WidgetProperty } from "../observable/property/WidgetProperty";
import { input } from "./dom";

export type CheckBoxOptions = LabelledControlOptions;

export class CheckBox extends LabelledControl {
    readonly element = input({ className: "core_CheckBox" });

    readonly preferred_label_position = "right";

    readonly checked: WritableProperty<boolean>;

    private readonly _checked: WidgetProperty<boolean>;

    constructor(checked: boolean = false, options?: CheckBoxOptions) {
        super(options);

        this._checked = new WidgetProperty(this, checked, this.set_checked);
        this.checked = this._checked;
        this.set_checked(checked);

        this.element.type = "checkbox";
        this.element.onchange = () =>
            this._checked.set_val(this.element.checked, { silent: false });

        this.finalize_construction(CheckBox);
    }

    protected set_enabled(enabled: boolean): void {
        super.set_enabled(enabled);
        this.element.disabled = !enabled;
    }

    protected set_checked(checked: boolean): void {
        this.element.checked = checked;
    }
}
