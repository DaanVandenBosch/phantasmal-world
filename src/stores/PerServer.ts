import { Server } from "../domain";
import { computed } from "mobx";
import { applicationStore } from "./ApplicationStore";

/**
 * Represents a value per server.
 * E.g. drop tables differ per server, this can be represented by PerServer<DropTable>.
 */
export class PerServer<T> {
    private values = new Map<Server, T>();

    constructor(initialValue: T | ((server: Server) => T)) {
        if (!(initialValue instanceof Function)) {
            this.values.set(Server.Ephinea, initialValue);
        } else {
            this.values.set(Server.Ephinea, initialValue(Server.Ephinea));
        }
    }

    get(server: Server): T {
        return this.values.get(server)!;
    }

    /**
     * @returns the value for the current server as set in {@link applicationStore}.
     */
    @computed get current(): T {
        return this.get(applicationStore.currentServer);
    }
}
