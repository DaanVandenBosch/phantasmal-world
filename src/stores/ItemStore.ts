import { observable } from "mobx";
import { Item, Server } from "../domain";
import { Loadable } from "../Loadable";
import { ServerMap } from "./ServerMap";

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
            `${process.env.PUBLIC_URL}/items.${Server[server].toLowerCase()}.tsv`
        );
        const data = await response.text();
        return data.split('\n').slice(1).map(name => this.dedupItem(name));
    }
}

export const itemStore = new ItemStore();
