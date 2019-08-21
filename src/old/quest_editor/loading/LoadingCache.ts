export class LoadingCache<K, V> {
    private map = new Map<K, V>();

    set(key: K, value: V): void {
        this.map.set(key, value);
    }

    get_or_set(key: K, new_value: () => V): V {
        let v = this.map.get(key);

        if (v === undefined) {
            v = new_value();
            this.map.set(key, v);
        }

        return v;
    }
}
