import { Widget } from "../../core/gui/Widget";
import { el } from "../../core/gui/dom";
import { Label } from "../../core/gui/Label";
import "./DisabledView.css";

export class DisabledView extends Widget {
    private readonly label: Label;

    constructor(text: string) {
        super(el.div({ class: "quest_editor_DisabledView" }));

        this.label = this.disposable(new Label(text, { enabled: false }));

        this.element.append(this.label.element);

        this.finalize_construction(DisabledView.prototype);
    }
}
