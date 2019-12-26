import { Widget } from "../../core/gui/Widget";
import { el } from "../../core/gui/dom";
import { Label } from "../../core/gui/Label";
import "./UnavailableView.css";

/**
 * Used to show that a view exists but is unavailable at the moment.
 */
export class UnavailableView extends Widget {
    readonly element = el.div({ class: "quest_editor_UnavailableView" });

    private readonly label: Label;

    constructor(message: string) {
        super();

        this.label = this.disposable(new Label(message, { enabled: false }));

        this.element.append(this.label.element);

        this.finalize_construction();
    }
}
