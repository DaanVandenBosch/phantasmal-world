import "./HelpView.css";
import { div, p } from "../../core/gui/dom";
import { ResizableView } from "../../core/gui/ResizableView";

export class HelpView extends ResizableView {
    readonly element = div(
        { className: "hunt_optimizer_HelpView" },
        p(
            "Add some items with the combo box on the left to see the optimal combination of hunt methods on the right.",
        ),
        p(
            'At the moment a hunt method is simply a quest run-through. Partial quest run-throughs are coming. View the list of methods on the "Methods" tab. Each method takes a certain amount of time, which affects the optimization result. Make sure the times are correct for you.',
        ),
        p("Only enemy drops are considered. Box drops are coming."),
        p(
            "The optimal result is calculated using linear optimization. The optimizer takes into account rare enemies and the fact that pan arms can be split in two.",
        ),
    );

    constructor() {
        super();
        this.finalize_construction();
    }
}
