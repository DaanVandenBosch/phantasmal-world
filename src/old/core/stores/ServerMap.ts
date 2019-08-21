import { computed } from "mobx";
import { Server } from "../../../core/domain";
import { EnumMap } from "../../../core/enums";

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
        return this.get(Server.Ephinea);
    }
}
