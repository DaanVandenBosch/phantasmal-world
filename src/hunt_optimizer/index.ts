import { HuntOptimizerView } from "./gui/HuntOptimizerView";
import { ServerMap } from "../core/stores/ServerMap";
import { create_hunt_method_stores, HuntMethodStore } from "./stores/HuntMethodStore";
import { GuiStore } from "../core/stores/GuiStore";
import { create_hunt_optimizer_stores, HuntOptimizerStore } from "./stores/HuntOptimizerStore";
import { ItemTypeStore } from "../core/stores/ItemTypeStore";
import { HuntMethodPersister } from "./persistence/HuntMethodPersister";
import { HuntOptimizerPersister } from "./persistence/HuntOptimizerPersister";
import { ItemDropStore } from "./stores/ItemDropStore";
import { HttpClient } from "../core/HttpClient";
import { Disposable } from "../core/observable/Disposable";
import { Disposer } from "../core/observable/Disposer";

export function initialize_hunt_optimizer(
    http_client: HttpClient,
    gui_store: GuiStore,
    item_type_stores: ServerMap<ItemTypeStore>,
    item_drop_stores: ServerMap<ItemDropStore>,
): { view: HuntOptimizerView } & Disposable {
    const disposer = new Disposer();

    const hunt_method_stores: ServerMap<HuntMethodStore> = disposer.add(
        create_hunt_method_stores(http_client, gui_store, new HuntMethodPersister()),
    );
    const hunt_optimizer_stores: ServerMap<HuntOptimizerStore> = disposer.add(
        create_hunt_optimizer_stores(
            gui_store,
            new HuntOptimizerPersister(item_type_stores),
            item_type_stores,
            item_drop_stores,
            hunt_method_stores,
        ),
    );

    const view = disposer.add(
        new HuntOptimizerView(gui_store, hunt_optimizer_stores, hunt_method_stores),
    );

    return {
        view,
        dispose(): void {
            disposer.dispose();
        },
    };
}
