import { observable } from "mobx";
import { ItemKind, Server, WeaponItemKind, ArmorItemKind, ShieldItemKind, ToolItemKind, UnitItemKind } from "../domain";
import { Loadable } from "../Loadable";
import { ServerMap } from "./ServerMap";
import { ItemKindDto } from "../dto";

class ItemKindStore {
    private idToItemKind: Array<ItemKind> = [];

    @observable itemKinds: Array<ItemKind> = [];

    getById(id: number): ItemKind | undefined {
        return this.idToItemKind[id];
    }

    load = async (server: Server): Promise<ItemKindStore> => {
        const response = await fetch(
            `${process.env.PUBLIC_URL}/itemKinds.${Server[server].toLowerCase()}.json`
        );
        const data: Array<ItemKindDto> = await response.json();

        const itemKinds = new Array<ItemKind>();

        for (const itemKindDto of data) {
            let itemKind: ItemKind;

            switch (itemKindDto.type) {
                case 'weapon':
                    itemKind = new WeaponItemKind(
                        itemKindDto.id,
                        itemKindDto.name,
                        itemKindDto.minAtp,
                        itemKindDto.maxAtp,
                        itemKindDto.ata,
                        itemKindDto.maxGrind,
                        itemKindDto.requiredAtp,
                    );
                    break;
                case 'armor':
                    itemKind = new ArmorItemKind(
                        itemKindDto.id,
                        itemKindDto.name,
                    );
                    break;
                case 'shield':
                    itemKind = new ShieldItemKind(
                        itemKindDto.id,
                        itemKindDto.name,
                    );
                    break;
                case 'unit':
                    itemKind = new UnitItemKind(
                        itemKindDto.id,
                        itemKindDto.name,
                    );
                    break;
                case 'tool':
                    itemKind = new ToolItemKind(
                        itemKindDto.id,
                        itemKindDto.name,
                    );
                    break;
                default:
                    continue;
            }

            this.idToItemKind[itemKind.id] = itemKind;
            itemKinds.push(itemKind);
        }

        this.itemKinds = itemKinds;

        return this;
    }
}

export const itemKindStores: ServerMap<Loadable<ItemKindStore>> = new ServerMap(server => {
    const store = new ItemKindStore();
    return new Loadable(store, () => store.load(server));
});
