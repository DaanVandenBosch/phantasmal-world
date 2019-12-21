import {
    ArmorItemType,
    ItemType,
    ShieldItemType,
    ToolItemType,
    UnitItemType,
    WeaponItemType,
} from "../model/items";
import { ServerMap } from "./ServerMap";
import { Server } from "../model";
import { ItemTypeDto } from "../dto/ItemTypeDto";
import { GuiStore } from "./GuiStore";
import { HttpClient } from "../HttpClient";

export function load_item_type_stores(
    http_client: HttpClient,
    gui_store: GuiStore,
): ServerMap<ItemTypeStore> {
    return new ServerMap(gui_store, create_loader(http_client));
}

export class ItemTypeStore {
    readonly item_types: ItemType[];

    constructor(item_types: ItemType[], private readonly id_to_item_type: ItemType[]) {
        this.item_types = item_types;
    }

    get_by_id = (id: number): ItemType | undefined => {
        return this.id_to_item_type[id];
    };
}

function create_loader(http_client: HttpClient): (server: Server) => Promise<ItemTypeStore> {
    return async server => {
        const data: ItemTypeDto[] = await http_client
            .get(`/itemTypes.${Server[server].toLowerCase()}.json`)
            .json();
        const item_types: ItemType[] = [];
        const id_to_item_type: ItemType[] = [];

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
                        item_type_dto.requiredAtp,
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
                        item_type_dto.lck,
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
                        item_type_dto.lck,
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

            id_to_item_type[item_type.id] = item_type;
            item_types.push(item_type);
        }

        return new ItemTypeStore(item_types, id_to_item_type);
    };
}
