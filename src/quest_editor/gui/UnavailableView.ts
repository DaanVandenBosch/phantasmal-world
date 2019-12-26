import { Widget } from "../../core/gui/Widget";
import { Label } from "../../core/gui/Label";
import "./UnavailableView.css";
import { div } from "../../core/gui/dom";

/**
 * Used to show that a view exists but is unavailable at the moment.
 */
export class UnavailableView extends Widget {
    readonly element = div({ className: "quest_editor_UnavailableView" });

    private readonly label: Label;

    constructor(message: string) {
        super();

        this.label = this.disposable(new Label(message, { enabled: false }));

        this.element.append(this.label.element);

        this.finalize_construction();
    }
}
