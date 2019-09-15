import { Label } from "./Label";
import { Control } from "./Control";
import { WidgetOptions } from "./Widget";

export type LabelledControlOptions = WidgetOptions & {
    label?: string;
};

export type LabelPosition = "left" | "right" | "top" | "bottom";

export abstract class LabelledControl extends Control {
    abstract readonly preferred_label_position: LabelPosition;

    get label(): Label | undefined {
        if (!this._label && this._label_text != undefined) {
            this._label = this.disposable(
                new Label(this._label_text, {
                    enabled: this.enabled.val,
                    tooltip: this.tooltip.val,
                }),
            );

            if (!this.id) {
                this.id = unique_id();
            }

            this._label.for = this.id;
        }

        return this._label;
    }

    private readonly _label_text?: string;
    private _label?: Label;

    protected constructor(options?: LabelledControlOptions) {
        super(options);

        this._label_text = options && options.label;
    }

    protected set_enabled(enabled: boolean): void {
        super.set_enabled(enabled);

        if (this._label) {
            this._label.enabled.val = enabled;
        }
    }

    protected set_tooltip(tooltip: string): void {
        super.set_tooltip(tooltip);

        if (this._label) {
            this._label.tooltip.val = tooltip;
        }
    }
}

let id = 0;

function unique_id(): string {
    return "core_LabelledControl_id_" + String(id++);
}
