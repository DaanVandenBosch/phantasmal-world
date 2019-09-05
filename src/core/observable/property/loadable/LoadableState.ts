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
