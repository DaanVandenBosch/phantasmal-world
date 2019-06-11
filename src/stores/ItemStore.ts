import { observable } from "mobx";
import { Item, Server } from "../domain";
import { Loadable } from "../Loadable";
import { ServerMap } from "./ServerMap";

type ItemDTO = { name: string }

class ItemStore {
    private itemMap = new Map<string, Item>();

    @observable items: ServerMap<Loadable<Array<Item>>> = new ServerMap(server =>
        new Loadable([], () => this.loadItems(server))
    );

    dedupItem = (name: string): Item => {
        let item = this.itemMap.get(name);

        if (!item) {
            this.itemMap.set(name, item = new Item(name));
        }

        return item;
    }

    private async loadItems(server: Server): Promise<Item[]> {
        const response = await fetch(
            `${process.env.PUBLIC_URL}/items.${Server[server].toLowerCase()}.json`
        );
        const data: Array<ItemDTO> = await response.json();
        return data.map(({ name }) => this.dedupItem(name));
    }
}

export const itemStore = new ItemStore();
