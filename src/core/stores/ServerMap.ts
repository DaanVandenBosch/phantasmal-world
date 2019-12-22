import { Server } from "../model";
import { Property } from "../observable/property/Property";
import { memoize } from "lodash";
import { GuiStore } from "./GuiStore";

/**
 * Map with a lazily-loaded, guaranteed value per server.
 */
export class ServerMap<T> {
    /**
     * The value for the current server as set in the {@link GuiStore}.
     */
    get current(): Property<Promise<T>> {
        if (!this._current) {
            this._current = this.gui_store.server.map(server => this.get(server));
        }

        return this._current;
    }

    private readonly get_value: (server: Server) => Promise<T>;
    private _current?: Property<Promise<T>>;

    constructor(private readonly gui_store: GuiStore, get_value: (server: Server) => Promise<T>) {
        this.get_value = memoize(get_value);
    }

    get(server: Server): Promise<T> {
        return this.get_value(server);
    }
}
