import { ServerModel } from "../model";
import { EnumMap } from "../enums";
import { Property } from "../observable/property/Property";
import { gui_store } from "./GuiStore";

/**
 * Map with a guaranteed value per server.
 */
export class ServerMap<V> extends EnumMap<ServerModel, V> {
    /**
     * @returns the value for the current server as set in {@link gui_store}.
     */
    readonly current: Property<V>;

    constructor(initial_value: (server: ServerModel) => V) {
        super(ServerModel, initial_value);

        this.current = gui_store.server.map(server => this.get(server));
    }
}
