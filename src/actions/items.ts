import { memoize } from "lodash";
import { getItems } from "../data/loading/items";
import { itemStore } from "../stores/ItemStore";

export const loadItems = memoize(
    async (server: string) => {
        itemStore.items.replace(await getItems(server));
    }
);
