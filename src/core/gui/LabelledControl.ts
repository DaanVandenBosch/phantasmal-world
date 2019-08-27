import { Label } from "./Label";
import { Control } from "./Control";
import { ViewOptions } from "./Widget";

export type LabelledControlOptions = ViewOptions & {
    label?: string;
};

export abstract class LabelledControl extends Control {
    abstract readonly preferred_label_position: "left" | "right" | "top" | "bottom";

    get label(): Label {
        if (!this._label) {
            this._label = this.disposable(new Label(this._label_text));

            if (!this.id) {
                this._label.for = this.id = unique_id();
            }

            this._label.enabled.bind_bi(this.enabled);
        }

        return this._label;
    }

    private readonly _label_text: string;
    private _label?: Label;

    protected constructor(options?: LabelledControlOptions) {
        super(options);

        this._label_text = (options && options.label) || "";
    }
}

let id = 0;

function unique_id(): string {
    return String(id++);
}
