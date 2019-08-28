import { Label } from "./Label";
import { Control } from "./Control";
import { WidgetOptions } from "./Widget";

export type LabelledControlOptions = WidgetOptions & {
    label?: string;
};

export abstract class LabelledControl<E extends HTMLElement = HTMLElement> extends Control<E> {
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

    protected constructor(element: E, options?: LabelledControlOptions) {
        super(element, options);

        this._label_text = (options && options.label) || "";
    }
}

let id = 0;

function unique_id(): string {
    return String(id++);
}
