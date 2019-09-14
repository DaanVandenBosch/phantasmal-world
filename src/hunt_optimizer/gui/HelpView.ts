import { el } from "../../core/gui/dom";
import { ResizableWidget } from "../../core/gui/ResizableWidget";
import "./HelpView.css";

export class HelpView extends ResizableWidget {
    constructor() {
        super(
            el.div(
                { class: "hunt_optimizer_HelpView" },
                el.p({
                    text:
                        "Add some items with the combo box on the left to see the optimal combination of hunt methods on the right.",
                }),
                el.p({
                    text:
                        'At the moment a hunt method is simply a quest run-through. Partial quest run-throughs are coming. View the list of methods on the "Methods" tab. Each method takes a certain amount of time, which affects the optimization result. Make sure the times are correct for you.',
                }),
                el.p({ text: "Only enemy drops are considered. Box drops are coming." }),
                el.p({
                    text:
                        "The optimal result is calculated using linear optimization. The optimizer takes into account rare enemies and the fact that pan arms can be split in two.",
                }),
            ),
        );

        this.finalize_construction(HelpView.prototype);
    }
}
