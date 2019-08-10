import { computed } from "mobx";
import { Server } from "../domain";
import { application_store } from "../../application/stores/ApplicationStore";
import { EnumMap } from "../enums";

/**
 * Map with a guaranteed value per server.
 */
export class ServerMap<V> extends EnumMap<Server, V> {
    constructor(initial_value: (server: Server) => V) {
        super(Server, initial_value);
    }

    /**
     * @returns the value for the current server as set in {@link application_store}.
     */
    @computed get current(): V {
        return this.get(application_store.current_server);
    }
}
