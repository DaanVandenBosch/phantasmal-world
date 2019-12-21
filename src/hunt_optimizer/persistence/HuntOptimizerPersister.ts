import { Server } from "../../core/model";
import { Persister } from "../../core/persistence";
import { WantedItemModel } from "../model";
import { ItemTypeStore } from "../../core/stores/ItemTypeStore";
import { ServerMap } from "../../core/stores/ServerMap";

const WANTED_ITEMS_KEY = "HuntOptimizerStore.wantedItems";

export class HuntOptimizerPersister extends Persister {
    constructor(private readonly item_type_stores: ServerMap<ItemTypeStore>) {
        super();
    }

    persist_wanted_items(server: Server, wanted_items: readonly WantedItemModel[]): void {
        this.persist_for_server(
            server,
            WANTED_ITEMS_KEY,
            wanted_items.map(
                ({ item_type, amount }): PersistedWantedItem => ({
                    itemTypeId: item_type.id,
                    amount: amount.val,
                }),
            ),
        );
    }

    async load_wanted_items(server: Server): Promise<WantedItemModel[]> {
        const item_store = await this.item_type_stores.get(server);

        const persisted_wanted_items = await this.load_for_server<PersistedWantedItem[]>(
            server,
            WANTED_ITEMS_KEY,
        );
        const wanted_items: WantedItemModel[] = [];

        if (persisted_wanted_items) {
            for (const { itemTypeId, itemKindId, amount } of persisted_wanted_items) {
                const item =
                    itemTypeId != undefined
                        ? item_store.get_by_id(itemTypeId)
                        : item_store.get_by_id(itemKindId!);

                if (item) {
                    wanted_items.push(new WantedItemModel(item, amount));
                }
            }
        }

        return wanted_items;
    }
}

type PersistedWantedItem = {
    itemTypeId?: number; // Should only be undefined if the legacy name is still used.
    itemKindId?: number; // Legacy name, not persisted, only checked when loading.
    amount: number;
};
