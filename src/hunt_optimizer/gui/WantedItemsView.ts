import { el, Icon } from "../../core/gui/dom";
import "./WantedItemsView.css";
import { hunt_optimizer_store } from "../stores/HuntOptimizerStore";
import { Button } from "../../core/gui/Button";
import { Disposer } from "../../core/observable/Disposer";
import { Widget } from "../../core/gui/Widget";
import {
    ListChangeType,
    ListPropertyChangeEvent,
} from "../../core/observable/property/list/ListProperty";
import { WantedItemModel } from "../model";
import { NumberInput } from "../../core/gui/NumberInput";

export class WantedItemsView extends Widget {
    private readonly tbody_element = el.tbody();
    private readonly table_disposer = this.disposable(new Disposer());

    constructor() {
        super(el.div({ class: "hunt_optimizer_WantedItemsView" }));

        this.element.append(
            el.h2({ text: "Wanted Items" }),
            el.div(
                { class: "hunt_optimizer_WantedItemsView_table_wrapper" },
                el.table({}, this.tbody_element),
            ),
        );

        hunt_optimizer_store.wanted_items.observe_list(this.update_table, { call_now: true });
    }

    private update_table = (change: ListPropertyChangeEvent<WantedItemModel>): void => {
        switch (change.type) {
            case ListChangeType.Insertion:
                {
                    const rows = change.inserted.map(this.create_row);

                    if (change.from >= this.tbody_element.childElementCount) {
                        this.tbody_element.append(...rows);
                    } else {
                        for (let i = change.from; i < change.to; i++) {
                            this.tbody_element.children[i].insertAdjacentElement(
                                "afterend",
                                rows[i - change.from],
                            );
                        }
                    }
                }
                break;

            case ListChangeType.Removal:
                for (let i = change.from; i < change.to; i++) {
                    this.tbody_element.children[change.from].remove();
                }
                break;

            case ListChangeType.Replacement:
                {
                    const rows = change.inserted.map(this.create_row);

                    for (let i = change.from; i < change.removed_to; i++) {
                        this.tbody_element.children[change.from].remove();
                    }

                    if (change.from >= this.tbody_element.childElementCount) {
                        this.tbody_element.append(...rows);
                    } else {
                        for (let i = change.from; i < change.inserted_to; i++) {
                            this.tbody_element.children[i].insertAdjacentElement(
                                "afterend",
                                rows[i - change.from],
                            );
                        }
                    }
                }
                break;

            case ListChangeType.Update:
                // TODO: update row
                break;
        }
    };

    private create_row = (wanted_item: WantedItemModel): HTMLTableRowElement => {
        const amount_input = this.table_disposer.add(
            new NumberInput(wanted_item.amount.val, { min: 1, step: 1 }),
        );

        this.table_disposer.add_all(
            amount_input.value.bind_to(wanted_item.amount),
            amount_input.value.observe(({ value }) => wanted_item.set_amount(value)),
        );

        const remove_button = this.table_disposer.add(new Button("", { icon_left: Icon.Remove }));

        return el.tr(
            {},
            el.td({}, amount_input.element),
            el.td({ text: wanted_item.item_type.name }),
            el.td({}, remove_button.element),
        );
    };
}
