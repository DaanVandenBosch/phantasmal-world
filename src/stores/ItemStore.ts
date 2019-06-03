import { sortedUniq } from "lodash";
import { observable } from "mobx";
import { Item, Server } from "../domain";
import { Loadable } from "../Loadable";
import { PerServer } from "./PerServer";

class ItemStore {
    @observable items: PerServer<Loadable<Array<Item>>> = new PerServer(server =>
        new Loadable([], () => this.loadItems(server))
    );

    private async loadItems(server: Server): Promise<Item[]> {
        const response = await fetch(process.env.PUBLIC_URL + `/drops.${Server[server]}.tsv`);
        const data = await response.text();
        return sortedUniq(
            data.split('\n').slice(1).map(line => line.split('\t')[4]).sort()
        ).map(name => new Item(name));
    }
}

export const itemStore = new ItemStore();
