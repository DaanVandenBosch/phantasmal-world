import { Property } from "../Property";
import { WritableProperty } from "../WritableProperty";
import { property } from "../../index";
import { AbstractProperty } from "../AbstractProperty";
import { LoadableState } from "./LoadableState";
import { LoadableProperty } from "./LoadableProperty";

export class SimpleLoadableProperty<T> extends AbstractProperty<T> implements LoadableProperty<T> {
    get val(): T {
        return this.get_val();
    }

    readonly state: Property<LoadableState>;
    readonly is_initialized: Property<boolean>;
    readonly is_loading: Property<boolean>;

    get promise(): Promise<T> {
        // Load value on first use.
        if (this._state.val === LoadableState.Uninitialized) {
            return this.load_value();
        } else {
            return this._promise;
        }
    }

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
        return this._val;
    }

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
