import { observable } from "mobx";
import {
    ItemType,
    Server,
    WeaponItemType,
    ArmorItemType,
    ShieldItemType,
    ToolItemType,
    UnitItemType,
} from "../domain";
import { Loadable } from "../Loadable";
import { ServerMap } from "./ServerMap";
import { ItemTypeDto } from "../dto";

export class ItemTypeStore {
    private id_to_item_type: Array<ItemType> = [];

    @observable item_types: Array<ItemType> = [];

    get_by_id(id: number): ItemType | undefined {
        return this.id_to_item_type[id];
    }

    load = async (server: Server): Promise<ItemTypeStore> => {
        const response = await fetch(
            `${process.env.PUBLIC_URL}/itemTypes.${Server[server].toLowerCase()}.json`
        );
        const data: Array<ItemTypeDto> = await response.json();

        const item_types = new Array<ItemType>();

        for (const item_type_dto of data) {
            let item_type: ItemType;

            switch (item_type_dto.class) {
                case "weapon":
                    item_type = new WeaponItemType(
                        item_type_dto.id,
                        item_type_dto.name,
                        item_type_dto.minAtp,
                        item_type_dto.maxAtp,
                        item_type_dto.ata,
                        item_type_dto.maxGrind,
                        item_type_dto.requiredAtp
                    );
                    break;
                case "armor":
                    item_type = new ArmorItemType(
                        item_type_dto.id,
                        item_type_dto.name,
                        item_type_dto.atp,
                        item_type_dto.ata,
                        item_type_dto.minEvp,
                        item_type_dto.maxEvp,
                        item_type_dto.minDfp,
                        item_type_dto.maxDfp,
                        item_type_dto.mst,
                        item_type_dto.hp,
                        item_type_dto.lck
                    );
                    break;
                case "shield":
                    item_type = new ShieldItemType(
                        item_type_dto.id,
                        item_type_dto.name,
                        item_type_dto.atp,
                        item_type_dto.ata,
                        item_type_dto.minEvp,
                        item_type_dto.maxEvp,
                        item_type_dto.minDfp,
                        item_type_dto.maxDfp,
                        item_type_dto.mst,
                        item_type_dto.hp,
                        item_type_dto.lck
                    );
                    break;
                case "unit":
                    item_type = new UnitItemType(item_type_dto.id, item_type_dto.name);
                    break;
                case "tool":
                    item_type = new ToolItemType(item_type_dto.id, item_type_dto.name);
                    break;
                default:
                    continue;
            }

            this.id_to_item_type[item_type.id] = item_type;
            item_types.push(item_type);
        }

        this.item_types = item_types;

        return this;
    };
}

export const item_type_stores: ServerMap<Loadable<ItemTypeStore>> = new ServerMap(server => {
    const store = new ItemTypeStore();
    return new Loadable(store, () => store.load(server));
});
