import { computed } from "mobx";
import { Server } from "../domain";
import { applicationStore } from "./ApplicationStore";
import { EnumMap } from "../enums";

export class ServerMap<V> extends EnumMap<Server, V> {
    constructor(initialValue: V | ((server: Server) => V)) {
        super(Server, initialValue)
    }

    /**
     * @returns the value for the current server as set in {@link applicationStore}.
     */
    @computed get current(): V {
        return this.get(applicationStore.currentServer);
    }
}
