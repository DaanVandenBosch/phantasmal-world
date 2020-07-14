import { Label } from "../../core/gui/Label";
import "./UnavailableView.css";
import { div } from "../../core/gui/dom";
import { View } from "../../core/gui/View";

/**
 * Used to show that a view exists but is unavailable at the moment.
 */
export class UnavailableView extends View {
    readonly element = div({ className: "quest_editor_UnavailableView" });

    private readonly label: Label;

    constructor(message: string) {
        super();

        this.label = this.disposable(new Label(message, { enabled: false }));

        this.element.append(this.label.element);

        this.finalize_construction(UnavailableView);
    }
}
