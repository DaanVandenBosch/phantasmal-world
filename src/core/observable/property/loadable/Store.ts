import { LoadableState } from "./LoadableState";
import { Property } from "../Property";
import { WritableProperty } from "../WritableProperty";
import { property } from "../../index";

export class Store {
    readonly state: Property<LoadableState>;

    /**
     * True if the initial data load has happened. It may or may not have succeeded.
     * Check [error]{@link LoadableProperty#error} to know whether an error occurred.
     */
    readonly is_initialized: Property<boolean>;

    /**
     * True if a data load is underway. This may be the initializing load or a later reload.
     */
    readonly is_loading: Property<boolean>;

    /**
     * Contains the {@link Error} object if an error occurred during the most recent data load.
     */
    readonly error: Property<Error | undefined>;

    private readonly _state: WritableProperty<LoadableState> = property(
        LoadableState.Uninitialized,
    );
    private readonly _error: WritableProperty<Error | undefined> = property(undefined);

    constructor() {
        this.state = this._state;

        this.is_initialized = this.state.map(state => state !== LoadableState.Uninitialized);

        this.is_loading = this.state.map(
            state => state === LoadableState.Initializing || state === LoadableState.Reloading,
        );

        this.error = this._error;
    }
}
