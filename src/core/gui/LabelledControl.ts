import { Label } from "./Label";
import { Control } from "./Control";

export abstract class LabelledControl extends Control {
    abstract readonly preferred_label_position: "left" | "right";

    private readonly _label_text: string;
    private _label?: Label;

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

    protected constructor(label: string | undefined) {
        super();

        this._label_text = label || "";
    }
}

let id = 0;

function unique_id(): string {
    return String(id++);
}
