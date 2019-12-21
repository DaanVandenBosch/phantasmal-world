import { HuntOptimizerView } from "./gui/HuntOptimizerView";
import { ServerMap } from "../core/stores/ServerMap";
import { HuntMethodStore, load_hunt_method_stores } from "./stores/HuntMethodStore";
import { GuiStore } from "../core/stores/GuiStore";
import { HuntOptimizerStore, load_hunt_optimizer_stores } from "./stores/HuntOptimizerStore";
import { ItemTypeStore } from "../core/stores/ItemTypeStore";
import { HuntMethodPersister } from "./persistence/HuntMethodPersister";
import { HuntOptimizerPersister } from "./persistence/HuntOptimizerPersister";
import { ItemDropStore } from "./stores/ItemDropStore";
import { HttpClient } from "../core/HttpClient";

export function initialize_hunt_optimizer(
    http_client: HttpClient,
    gui_store: GuiStore,
    item_type_stores: ServerMap<ItemTypeStore>,
    item_drop_stores: ServerMap<ItemDropStore>,
): HuntOptimizerView {
    const hunt_method_stores: ServerMap<HuntMethodStore> = load_hunt_method_stores(
        http_client,
        gui_store,
        new HuntMethodPersister(),
    );
    const hunt_optimizer_stores: ServerMap<HuntOptimizerStore> = load_hunt_optimizer_stores(
        gui_store,
        new HuntOptimizerPersister(item_type_stores),
        item_type_stores,
        item_drop_stores,
        hunt_method_stores,
    );

    return new HuntOptimizerView(hunt_optimizer_stores, hunt_method_stores);
}
