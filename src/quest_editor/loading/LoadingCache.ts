import { DisposablePromise } from "../../core/DisposablePromise";
import { Disposable } from "../../core/observable/Disposable";

export class LoadingCache<K, V> implements Disposable {
    private map = new Map<K, DisposablePromise<V>>();

    set(key: K, value: DisposablePromise<V>): void {
        this.map.set(key, value);
    }

    get_or_set(key: K, new_value: () => DisposablePromise<V>): DisposablePromise<V> {
        let v = this.map.get(key);

        if (v === undefined) {
            v = new_value();
            this.map.set(key, v);
        }

        return v;
    }

    dispose(): void {
        for (const value of this.values()) {
            value.dispose();
        }

        this.map.clear();
    }

    values(): IterableIterator<DisposablePromise<V>> {
        return this.map.values();
    }
}
