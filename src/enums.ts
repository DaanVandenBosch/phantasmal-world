export function enumValues<E>(e: any): E[] {
    const values = Object.values(e);
    const numberValues = values.filter(v => typeof v === 'number');

    if (numberValues.length) {
        return numberValues as any as E[];
    } else {
        return values as any as E[];
    }
}

export function enumNames(e: any): string[] {
    return Object.keys(e).filter(k => typeof (e as any)[k] === 'string');
}

/**
 * Map with a guaranteed value per enum key.
 */
export class EnumMap<K, V> {
    private keys: K[];
    private values = new Map<K, V>();

    constructor(enum_: any, initialValue: (key: K) => V) {
        this.keys = enumValues(enum_);

        for (const key of this.keys) {
            this.values.set(key, initialValue(key));
        }
    }

    get(key: K): V {
        return this.values.get(key)!;
    }
}
