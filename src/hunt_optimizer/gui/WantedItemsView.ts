import { bind_children_to, el, Icon } from "../../core/gui/dom";
import "./WantedItemsView.css";
import { Button } from "../../core/gui/Button";
import { Disposer } from "../../core/observable/Disposer";
import { Widget } from "../../core/gui/Widget";
import { WantedItemModel } from "../model";
import { NumberInput } from "../../core/gui/NumberInput";
import { hunt_optimizer_stores } from "../stores/HuntOptimizerStore";
import { ComboBox } from "../../core/gui/ComboBox";
import { list_property } from "../../core/observable";
import { ItemType } from "../../core/model/items";
import { Disposable } from "../../core/observable/Disposable";

export class WantedItemsView extends Widget {
    readonly element = el.div({ class: "hunt_optimizer_WantedItemsView" });

    private readonly tbody_element = el.tbody();
    private readonly store_disposer = this.disposable(new Disposer());

    constructor() {
        super();

        const huntable_items = list_property<ItemType>();
        const filtered_huntable_items = list_property<ItemType>();

        const combo_box = this.disposable(
            new ComboBox({
                items: filtered_huntable_items,
                to_label: item_type => item_type.name,
                placeholder_text: "Add an item",
                filter(text: string): void {
                    const text_lower = text.toLowerCase();
                    filtered_huntable_items.val = huntable_items.val.filter(item_type =>
                        item_type.name.toLowerCase().includes(text_lower),
                    );
                },
            }),
        );

        this.element.append(
            el.h2({ text: "Wanted Items" }),
            combo_box.element,
            el.div(
                { class: "hunt_optimizer_WantedItemsView_table_wrapper" },
                el.table({}, this.tbody_element),
            ),
        );

        this.disposables(
            hunt_optimizer_stores.observe_current(
                hunt_optimizer_store => {
                    this.store_disposer.dispose_all();

                    this.store_disposer.add_all(
                        bind_children_to(
                            this.tbody_element,
                            hunt_optimizer_store.wanted_items,
                            this.create_row,
                        ),

                        combo_box.selected.observe(({ value: item_type }) => {
                            if (item_type) {
                                hunt_optimizer_store.add_wanted_item(item_type);
                                combo_box.selected.val = undefined;
                            }
                        }),
                    );

                    huntable_items.val = hunt_optimizer_store.huntable_item_types
                        .slice()
                        .sort((a, b) => a.name.localeCompare(b.name));
                    filtered_huntable_items.val = huntable_items.val;
                },
                { call_now: true },
            ),
        );

        this.finalize_construction(WantedItemsView.prototype);
    }

    private create_row = (wanted_item: WantedItemModel): [HTMLTableRowElement, Disposable] => {
        const row_disposer = new Disposer();

        const amount_input = row_disposer.add(
            new NumberInput(wanted_item.amount.val, { min: 0, step: 1 }),
        );

        row_disposer.add_all(
            amount_input.value.bind_to(wanted_item.amount),
            amount_input.value.observe(({ value }) => wanted_item.set_amount(value)),
        );

        const remove_button = row_disposer.add(new Button("", { icon_left: Icon.Remove }));

        row_disposer.add(
            remove_button.click.observe(async () =>
                (await hunt_optimizer_stores.current.val).remove_wanted_item(wanted_item),
            ),
        );

        return [
            el.tr(
                {},
                el.td({}, amount_input.element),
                el.td({ text: wanted_item.item_type.name }),
                el.td({}, remove_button.element),
            ),
            row_disposer,
        ];
    };
}
