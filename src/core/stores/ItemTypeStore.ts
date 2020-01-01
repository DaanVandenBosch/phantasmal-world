import {
    ArmorItemType,
    ItemType,
    ShieldItemType,
    ToolItemType,
    UnitItemType,
    WeaponItemType,
} from "../model/items";
import { Server } from "../model";
import { ItemTypeDto } from "../dto/ItemTypeDto";
import { GuiStore } from "./GuiStore";
import { HttpClient } from "../HttpClient";
import { DisposableServerMap } from "./DisposableServerMap";
import { Store } from "./Store";

export function create_item_type_stores(
    http_client: HttpClient,
    gui_store: GuiStore,
): DisposableServerMap<ItemTypeStore> {
    return new DisposableServerMap(gui_store, create_loader(http_client));
}

export class ItemTypeStore extends Store {
    readonly item_types: ItemType[];

    constructor(item_types: ItemType[], private readonly id_to_item_type: ItemType[]) {
        super();
        this.item_types = item_types;
    }

    get_by_id = (id: number): ItemType | undefined => {
        return this.id_to_item_type[id];
    };
}

function create_loader(http_client: HttpClient): (server: Server) => Promise<ItemTypeStore> {
    return async server => {
        const data: ItemTypeDto[] = await http_client
            .get(`/item_types.${Server[server].toLowerCase()}.json`)
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
                        item_type_dto.min_atp,
                        item_type_dto.max_atp,
                        item_type_dto.ata,
                        item_type_dto.max_grind,
                        item_type_dto.required_atp,
                    );
                    break;
                case "armor":
                    item_type = new ArmorItemType(
                        item_type_dto.id,
                        item_type_dto.name,
                        item_type_dto.atp,
                        item_type_dto.ata,
                        item_type_dto.min_evp,
                        item_type_dto.max_evp,
                        item_type_dto.min_dfp,
                        item_type_dto.max_dfp,
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
                        item_type_dto.min_evp,
                        item_type_dto.max_evp,
                        item_type_dto.min_dfp,
                        item_type_dto.max_dfp,
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
