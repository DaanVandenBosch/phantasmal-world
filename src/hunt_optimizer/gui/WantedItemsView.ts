import { el, Icon } from "../../core/gui/dom";
import "./WantedItemsView.css";
import { Button } from "../../core/gui/Button";
import { Disposer } from "../../core/observable/Disposer";
import { Widget } from "../../core/gui/Widget";
import {
    ListChangeType,
    ListPropertyChangeEvent,
} from "../../core/observable/property/list/ListProperty";
import { WantedItemModel } from "../model";
import { NumberInput } from "../../core/gui/NumberInput";
import { ToolBar } from "../../core/gui/ToolBar";
import { hunt_optimizer_stores } from "../stores/HuntOptimizerStore";
import { Disposable } from "../../core/observable/Disposable";

export class WantedItemsView extends Widget {
    private readonly tbody_element = el.tbody();
    private readonly table_disposer = this.disposable(new Disposer());
    private wanted_items_observer?: Disposable;

    constructor() {
        super(el.div({ class: "hunt_optimizer_WantedItemsView" }));

        this.element.append(
            el.h2({ text: "Wanted Items" }),
            this.disposable(new ToolBar({ children: [new Button("Optimize")] })).element,
            el.div(
                { class: "hunt_optimizer_WantedItemsView_table_wrapper" },
                el.table({}, this.tbody_element),
            ),
        );

        this.disposable(
            hunt_optimizer_stores.observe_current(
                hunt_optimizer_store => {
                    if (this.wanted_items_observer) {
                        this.wanted_items_observer.dispose();
                    }

                    this.wanted_items_observer = hunt_optimizer_store.wanted_items.observe_list(
                        this.update_table,
                        {
                            call_now: true,
                        },
                    );
                },
                { call_now: true },
            ),
        );
    }

    dispose(): void {
        super.dispose();

        if (this.wanted_items_observer) {
            this.wanted_items_observer.dispose();
        }
    }

    private update_table = (change: ListPropertyChangeEvent<WantedItemModel>): void => {
        if (change.type === ListChangeType.ListChange) {
            for (let i = 0; i < change.removed.length; i++) {
                this.tbody_element.children[change.index].remove();
            }

            const rows = change.inserted.map(this.create_row);

            if (change.index >= this.tbody_element.childElementCount) {
                this.tbody_element.append(...rows);
            } else {
                for (let i = 0; i < change.inserted.length; i++) {
                    this.tbody_element.children[change.index + i].insertAdjacentElement(
                        "afterend",
                        rows[i],
                    );
                }
            }
        }
    };

    private create_row = (wanted_item: WantedItemModel): HTMLTableRowElement => {
        const amount_input = this.table_disposer.add(
            new NumberInput(wanted_item.amount.val, { min: 0, step: 1 }),
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
