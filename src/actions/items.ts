import { getItems } from "../data/loading/items";
import { itemStore } from "../stores/ItemStore";
import { action } from "mobx";
import { memoize } from "lodash";
import { Item } from "../domain";

export const loadItems = memoize(
    async (server: string) => {
        setItems(await getItems(server));
    }
);

const setItems = action('setItems', (items: Item[]) => {
    itemStore.items.replace(items);
});