import { Property } from "./Property";
import { WritableProperty } from "./WritableProperty";
import { property } from "../index";
import { AbstractProperty } from "./AbstractProperty";

export enum LoadableState {
    /**
     * No attempt has been made to load data.
     */
    Uninitialized,

    /**
     * The first data load is underway.
     */
    Initializing,

    /**
     * Data was loaded at least once. The most recent load was successful.
     */
    Nominal,

    /**
     * Data was loaded at least once. The most recent load failed.
     */
    Error,

    /**
     * Data was loaded at least once. Another data load is underway.
     */
    Reloading,
}

/**
 * Represents a value that can be loaded asynchronously.
 * [state]{@link Loadable#state} represents the current state of this Loadable's value.
 */
export class LoadableProperty<T> extends AbstractProperty<T> implements Property<T> {
    /**
     * When value is accessed and this Loadable is uninitialized, a load will be triggered.
     * Will return the initial value until a load has succeeded.
     */
    get val(): T {
        return this.get_val();
    }

    readonly state: Property<LoadableState>;

    /**
     * True if the initial data load has happened. It may or may not have succeeded.
     * Check [error]{@link Loadable#error} to know whether an error occurred.
     */
    readonly is_initialized: Property<boolean>;

    /**
     * True if a data load is underway. This may be the initializing load or a later reload.
     */
    readonly is_loading: Property<boolean>;

    /**
     * This property returns valid data as soon as possible.
     * If the Loadable is uninitialized a data load will be triggered, otherwise the current value will be returned.
     */
    get promise(): Promise<T> {
        // Load value on first use.
        if (this._state.val === LoadableState.Uninitialized) {
            return this.load_value();
        } else {
            return this._promise;
        }
    }

    /**
     * Contains the {@link Error} object if an error occurred during the most recent data load.
     */
    readonly error: Property<Error | undefined>;

    private _val: T;
    private _promise: Promise<T>;
    private readonly _state: WritableProperty<LoadableState> = property(
        LoadableState.Uninitialized,
    );
    private readonly _load?: () => Promise<T>;
    private readonly _error: WritableProperty<Error | undefined> = property(undefined);

    constructor(initial_value: T, load?: () => Promise<T>) {
        super();

        this._val = initial_value;
        this._promise = new Promise(resolve => resolve(this._val));
        this.state = this._state;

        this.is_initialized = this.state.map(state => state !== LoadableState.Uninitialized);

        this.is_loading = this.state.map(
            state => state === LoadableState.Initializing || state === LoadableState.Reloading,
        );

        this._load = load;
        this.error = this._error;
    }

    get_val(): T {
        // Load value on first use.
        if (this._state.val === LoadableState.Uninitialized) {
            this.load_value();
        }

        return this._val;
    }

    /**
     * Load the data. Initializes the Loadable if it is uninitialized.
     */
    load(): Promise<T> {
        return this.load_value();
    }

    private async load_value(): Promise<T> {
        if (this.is_loading.val) return this._promise;

        this._state.val = LoadableState.Initializing;
        const old_val = this._val;

        try {
            if (this._load) {
                this._promise = this._load();
                this._val = await this._promise;
            }

            this._state.val = LoadableState.Nominal;
            this._error.val = undefined;
            return this._val;
        } catch (e) {
            this._state.val = LoadableState.Error;
            this._error.val = e;
            throw e;
        } finally {
            this.emit(old_val);
        }
    }
}
