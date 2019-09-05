import { Property } from "../Property";
import { LoadableState } from "./LoadableState";

/**
 * Represents a value that can be loaded asynchronously.
 * [state]{@link LoadableProperty#state} represents the current state of this Loadable's value.
 */
export interface LoadableProperty<T> extends Property<T> {
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
     * This property returns valid data as soon as possible.
     * If the Loadable is uninitialized a data load will be triggered, otherwise the current value will be returned.
     */
    readonly promise: Promise<T>;

    /**
     * Contains the {@link Error} object if an error occurred during the most recent data load.
     */
    readonly error: Property<Error | undefined>;

    /**
     * Load the data. Initializes the Loadable if it is uninitialized.
     */
    load(): Promise<T>;
}
