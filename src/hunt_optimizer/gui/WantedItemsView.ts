import { bind_children_to, div, h2, Icon, table, tbody, td, tr } from "../../core/gui/dom";
import "./WantedItemsView.css";
import { Button } from "../../core/gui/Button";
import { Disposer } from "../../core/observable/Disposer";
import { WantedItemModel } from "../model";
import { NumberInput } from "../../core/gui/NumberInput";
import { ComboBox } from "../../core/gui/ComboBox";
import { list_property } from "../../core/observable";
import { ItemType } from "../../core/model/items";
import { Disposable } from "../../core/observable/Disposable";
import { ServerMap } from "../../core/stores/ServerMap";
import { HuntOptimizerStore } from "../stores/HuntOptimizerStore";
import { LogManager } from "../../core/Logger";
import { View } from "../../core/gui/View";

const logger = LogManager.get("hunt_optimizer/gui/WantedItemsView");

export class WantedItemsView extends View {
    private readonly tbody_element = tbody();
    private readonly store_disposer = this.disposable(new Disposer());

    readonly element = div({ className: "hunt_optimizer_WantedItemsView" });

    constructor(private readonly hunt_optimizer_stores: ServerMap<HuntOptimizerStore>) {
        super();

        const huntable_items = list_property<ItemType>();
        const filtered_huntable_items = list_property<ItemType>();

        const combo_box = this.add(
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
            h2("Wanted Items"),
            combo_box.element,
            div(
                { className: "hunt_optimizer_WantedItemsView_table_wrapper" },
                table(this.tbody_element),
            ),
        );

        this.disposables(
            hunt_optimizer_stores.current.observe(
                async ({ value }) => {
                    try {
                        const hunt_optimizer_store = await value;
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
                    } catch (e) {
                        logger.error("Couldn't load hunt optimizer store.", e);
                    }
                },
                { call_now: true },
            ),
        );

        this.finalize_construction(WantedItemsView);
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

        const remove_button = row_disposer.add(new Button({ icon_left: Icon.Remove }));

        row_disposer.add(
            remove_button.onclick.observe(async () =>
                (await this.hunt_optimizer_stores.current.val).remove_wanted_item(wanted_item),
            ),
        );

        return [
            tr(td(amount_input.element), td(wanted_item.item_type.name), td(remove_button.element)),
            row_disposer,
        ];
    };
}
