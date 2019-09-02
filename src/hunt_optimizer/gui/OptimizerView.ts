import { ResizableWidget } from "../../core/gui/ResizableWidget";
import { el } from "../../core/gui/dom";
import { WantedItemsView } from "./WantedItemsView";

export class OptimizerView extends ResizableWidget {
    private readonly wanted_items_view: WantedItemsView;

    constructor() {
        super(el.div({ class: "hunt_optimizer_OptimizerView" }));

        this.wanted_items_view = this.disposable(new WantedItemsView());
        this.element.append(this.wanted_items_view.element);
    }

    resize(width: number, height: number): this {
        super.resize(width, height);

        this.wanted_items_view.resize(Math.min(200, width), height);

        return this;
    }
}
