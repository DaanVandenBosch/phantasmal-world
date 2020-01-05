import { WantedItemsView } from "./WantedItemsView";
import "./OptimizerView.css";
import { OptimizationResultView } from "./OptimizationResultView";
import { ServerMap } from "../../core/stores/ServerMap";
import { HuntOptimizerStore } from "../stores/HuntOptimizerStore";
import { div } from "../../core/gui/dom";
import { ResizableView } from "../../core/gui/ResizableView";

export class OptimizerView extends ResizableView {
    readonly element = div({ className: "hunt_optimizer_OptimizerView" });

    constructor(hunt_optimizer_stores: ServerMap<HuntOptimizerStore>) {
        super();

        this.element.append(
            this.add(new WantedItemsView(hunt_optimizer_stores)).element,
            this.add(new OptimizationResultView(hunt_optimizer_stores)).element,
        );

        this.finalize_construction();
    }
}
