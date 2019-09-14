/**
 * Objects implementing this interface should be disposed when they're not used anymore.
 * This is to avoid e.g. memory leaks.
 */
export interface Disposable {
    /**
     * Releases any held resources.
     */
    dispose(): void;
}
