import { View } from "../../core/gui/View";
import { el } from "../../core/gui/dom";
import { Label } from "../../core/gui/Label";
import "./DisabledView.css";

export class DisabledView extends View {
    readonly element = el.div({ class: "quest_editor_DisabledView" });

    private readonly label: Label;

    constructor(text: string) {
        super();

        this.label = this.disposable(new Label(text, { enabled: false }));

        this.element.append(this.label.element);
    }
}
