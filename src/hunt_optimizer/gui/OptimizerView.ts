import { ResizableWidget } from "../../core/gui/ResizableWidget";
import { el } from "../../core/gui/dom";
import { WantedItemsView } from "./WantedItemsView";
import "./OptimizerView.css";
import { OptimizationResultView } from "./OptimizationResultView";

export class OptimizerView extends ResizableWidget {
    constructor() {
        super(el.div({ class: "hunt_optimizer_OptimizerView" }));

        this.element.append(
            this.disposable(new WantedItemsView()).element,
            this.disposable(new OptimizationResultView()).element,
        );
    }
}
