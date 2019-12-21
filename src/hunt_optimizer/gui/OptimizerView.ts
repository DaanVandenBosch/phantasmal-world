import { ResizableWidget } from "../../core/gui/ResizableWidget";
import { el } from "../../core/gui/dom";
import { WantedItemsView } from "./WantedItemsView";
import "./OptimizerView.css";
import { OptimizationResultView } from "./OptimizationResultView";
import { ServerMap } from "../../core/stores/ServerMap";
import { HuntOptimizerStore } from "../stores/HuntOptimizerStore";

export class OptimizerView extends ResizableWidget {
    readonly element = el.div({ class: "hunt_optimizer_OptimizerView" });

    constructor(hunt_optimizer_stores: ServerMap<HuntOptimizerStore>) {
        super();

        this.element.append(
            this.disposable(new WantedItemsView(hunt_optimizer_stores)).element,
            this.disposable(new OptimizationResultView(hunt_optimizer_stores)).element,
        );

        this.finalize_construction();
    }
}
