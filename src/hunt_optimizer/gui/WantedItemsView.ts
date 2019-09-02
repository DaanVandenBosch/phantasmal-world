import { ResizableWidget } from "../../core/gui/ResizableWidget";
import { el, Icon } from "../../core/gui/dom";
import "./WantedItemsView.css";
import { hunt_optimizer_store } from "../stores/HuntOptimizerStore";
import { Button } from "../../core/gui/Button";
import { Disposer } from "../../core/observable/Disposer";

export class WantedItemsView extends ResizableWidget {
    private readonly tbody_element = el.tbody();
    private readonly table_disposer = this.disposable(new Disposer());

    constructor() {
        super(el.div({ class: "hunt_optimizer_WantedItemsView" }));

        this.element.append(el.h2({ text: "Wanted Items" }), el.table({}, this.tbody_element));

        hunt_optimizer_store.wanted_items.observe_list(this.update_table);
    }

    private update_table = (): void => {
        this.tbody_element.append(
            ...hunt_optimizer_store.wanted_items.val.map(wanted_item => {
                const remove_button = this.table_disposer.add(
                    new Button("", { icon_left: Icon.Remove }),
                );

                return el.tr(
                    {},
                    el.td({ text: wanted_item.amount.toString() }),
                    el.td({ text: wanted_item.item_type.name }),
                    el.td({}, remove_button.element),
                );
            }),
        );
    };
}
