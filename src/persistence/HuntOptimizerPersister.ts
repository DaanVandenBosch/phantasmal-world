import { Server } from "../domain";
import { WantedItem } from "../stores/HuntOptimizerStore";
import { item_type_stores } from "../stores/ItemTypeStore";
import { Persister } from "./Persister";

const WANTED_ITEMS_KEY = "HuntOptimizerStore.wantedItems";

class HuntOptimizerPersister extends Persister {
    persist_wanted_items(server: Server, wanted_items: WantedItem[]): void {
        this.persist_for_server(
            server,
            WANTED_ITEMS_KEY,
            wanted_items.map(
                ({ item_type, amount }): PersistedWantedItem => ({
                    itemTypeId: item_type.id,
                    amount,
                })
            )
        );
    }

    async load_wanted_items(server: Server): Promise<WantedItem[]> {
        const item_store = await item_type_stores.get(server).promise;

        const persisted_wanted_items = await this.load_for_server<PersistedWantedItem[]>(
            server,
            WANTED_ITEMS_KEY
        );
        const wanted_items: WantedItem[] = [];

        if (persisted_wanted_items) {
            for (const { itemTypeId, itemKindId, amount } of persisted_wanted_items) {
                const item =
                    itemTypeId != undefined
                        ? item_store.get_by_id(itemTypeId)
                        : item_store.get_by_id(itemKindId!);

                if (item) {
                    wanted_items.push(new WantedItem(item, amount));
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

export const hunt_optimizer_persister = new HuntOptimizerPersister();
