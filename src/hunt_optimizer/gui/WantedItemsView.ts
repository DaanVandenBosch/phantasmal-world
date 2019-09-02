import { ResizableWidget } from "../../core/gui/ResizableWidget";
import { el } from "../../core/gui/dom";
import "./WantedItemsView.css";

export class WantedItemsView extends ResizableWidget {
    constructor() {
        super(el.div({ class: "hunt_optimizer_WantedItemsView" }));

        this.element.append(el.h2({ text: "Wanted Items" }));
    }
}
