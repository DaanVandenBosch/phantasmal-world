import { observable, computed } from "mobx";
import { defer } from "lodash";

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
export class Loadable<T> {
    @observable private _value: T;
    @observable private _promise: Promise<T> = new Promise(resolve => resolve(this._value));
    @observable private _state = LoadableState.Uninitialized;
    private _load?: () => Promise<T>;
    @observable private _error?: Error;

    constructor(initial_value: T, load?: () => Promise<T>) {
        this._value = initial_value;
        this._load = load;
    }

    /**
     * When this Loadable is uninitialized, a load will be triggered.
     * Will return the initial value until a load has succeeded.
     */
    @computed get value(): T {
        // Load value on first use and return initial placeholder value.
        if (this._state === LoadableState.Uninitialized) {
            // Defer loading value to avoid side effects in computed value.
            defer(() => this.load_value());
        }

        return this._value;
    }

    set value(value: T) {
        this._value = value;
    }

    /**
     * This property returns valid data as soon as possible.
     * If the Loadable is uninitialized a data load will be triggered, otherwise the current value will be returned.
     */
    get promise(): Promise<T> {
        // Load value on first use.
        if (this._state === LoadableState.Uninitialized) {
            return this.load_value();
        } else {
            return this._promise;
        }
    }

    @computed get state(): LoadableState {
        return this._state;
    }

    /**
     * @returns true if the initial data load has happened. It may or may not have succeeded.
     * Check [error]{@link Loadable#error} to know whether an error occurred.
     */
    @computed get is_initialized(): boolean {
        return this._state !== LoadableState.Uninitialized;
    }

    /**
     * @returns true if a data load is underway. This may be the initializing load or a later load.
     */
    @computed get is_loading(): boolean {
        switch (this._state) {
            case LoadableState.Initializing:
            case LoadableState.Reloading:
                return true;
            default:
                return false;
        }
    }

    /**
     * @returns an {@link Error} if an error occurred during the most recent data load.
     */
    @computed get error(): Error | undefined {
        return this._error;
    }

    /**
     * Load the data. Initializes the Loadable if it is uninitialized.
     */
    load(): Promise<T> {
        return this.load_value();
    }

    private async load_value(): Promise<T> {
        if (this.is_loading) return this._promise;

        this._state = LoadableState.Initializing;

        try {
            if (this._load) {
                this._promise = this._load();
                this._value = await this._promise;
            }

            this._state = LoadableState.Nominal;
            this._error = undefined;
            return this._value;
        } catch (e) {
            this._state = LoadableState.Error;
            this._error = e;
            throw e;
        }
    }
}
