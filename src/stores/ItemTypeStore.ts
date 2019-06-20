import { observable } from "mobx";
import { ItemType, Server, WeaponItemType, ArmorItemType, ShieldItemType, ToolItemType, UnitItemType } from "../domain";
import { Loadable } from "../Loadable";
import { ServerMap } from "./ServerMap";
import { ItemTypeDto } from "../dto";

class ItemTypeStore {
    private idToItemType: Array<ItemType> = [];

    @observable itemTypes: Array<ItemType> = [];

    getById(id: number): ItemType | undefined {
        return this.idToItemType[id];
    }

    load = async (server: Server): Promise<ItemTypeStore> => {
        const response = await fetch(
            `${process.env.PUBLIC_URL}/itemTypes.${Server[server].toLowerCase()}.json`
        );
        const data: Array<ItemTypeDto> = await response.json();

        const itemTypes = new Array<ItemType>();

        for (const itemTypeDto of data) {
            let itemType: ItemType;

            switch (itemTypeDto.class) {
                case 'weapon':
                    itemType = new WeaponItemType(
                        itemTypeDto.id,
                        itemTypeDto.name,
                        itemTypeDto.minAtp,
                        itemTypeDto.maxAtp,
                        itemTypeDto.ata,
                        itemTypeDto.maxGrind,
                        itemTypeDto.requiredAtp,
                    );
                    break;
                case 'armor':
                    itemType = new ArmorItemType(
                        itemTypeDto.id,
                        itemTypeDto.name,
                    );
                    break;
                case 'shield':
                    itemType = new ShieldItemType(
                        itemTypeDto.id,
                        itemTypeDto.name,
                    );
                    break;
                case 'unit':
                    itemType = new UnitItemType(
                        itemTypeDto.id,
                        itemTypeDto.name,
                    );
                    break;
                case 'tool':
                    itemType = new ToolItemType(
                        itemTypeDto.id,
                        itemTypeDto.name,
                    );
                    break;
                default:
                    continue;
            }

            this.idToItemType[itemType.id] = itemType;
            itemTypes.push(itemType);
        }

        this.itemTypes = itemTypes;

        return this;
    }
}

export const itemTypeStores: ServerMap<Loadable<ItemTypeStore>> = new ServerMap(server => {
    const store = new ItemTypeStore();
    return new Loadable(store, () => store.load(server));
});
