import { Server } from "../model";
import { Property } from "../observable/property/Property";
import { gui_store } from "./GuiStore";
import { memoize } from "lodash";
import { sequential } from "../util";
import { Disposable } from "../observable/Disposable";

/**
 * Map with a lazily-loaded, guaranteed value per server.
 */
export class ServerMap<T> {
    /**
     * The value for the current server as set in {@link gui_store}.
     */
    get current(): Property<Promise<T>> {
        if (!this._current) {
            this._current = gui_store.server.map(server => this.get(server));
        }

        return this._current;
    }

    private readonly get_value: (server: Server) => Promise<T>;
    private _current?: Property<Promise<T>>;

    constructor(get_value: (server: Server) => Promise<T>) {
        this.get_value = memoize(get_value);
    }

    get(server: Server): Promise<T> {
        return this.get_value(server);
    }

    observe_current(f: (current: T) => void, options?: { call_now?: boolean }): Disposable {
        const seq_f = sequential(async ({ value }: { value: Promise<T> }) => f(await value));
        return this.current.observe(seq_f, options);
    }
}
