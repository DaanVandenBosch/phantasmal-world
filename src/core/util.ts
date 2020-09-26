export function arrays_equal<T>(
    a: readonly T[],
    b: readonly T[],
    equal: (element_a: T, element_b: T) => boolean = (a, b) => a === b,
): boolean {
    const len = a.length;

    if (len !== b.length) return false;

    for (let i = 0; i < len; i++) {
        if (!equal(a[i], b[i])) return false;
    }

    return true;
}

/**
 * Removes 0 or more elements from `array`.
 *
 * @returns The number of removed elements.
 */
export function array_remove<T>(array: T[], ...elements: T[]): number {
    let count = 0;

    for (const element of elements) {
        const index = array.indexOf(element);

        if (index !== -1) {
            array.splice(index, 1);
            count++;
        }
    }

    return count;
}

export function array_buffers_equal(a: ArrayBuffer, b: ArrayBuffer): boolean {
    if (a.byteLength !== b.byteLength) return false;

    const a_arr = new Uint8Array(a);
    const b_arr = new Uint8Array(b);

    for (let i = 0; i < a_arr.length; i++) {
        if (a_arr[i] !== b_arr[i]) return false;
    }

    return true;
}

export function map_get_or_put<K, V>(map: Map<K, V>, key: K, get_default: () => V): V {
    let value = map.get(key);

    if (value === undefined) {
        value = get_default();
        map.set(key, value);
    }

    return value;
}

/**
 * Returns the given filename without the file extension.
 */
export function basename(filename: string): string {
    const dot_idx = filename.lastIndexOf(".");

    // < 0 means filename doesn't contain any "."
    // also skip index 0 because that would mean the basename is empty
    if (dot_idx > 1) {
        return filename.slice(0, dot_idx);
    }

    return filename;
}

export function filename_extension(filename: string): string {
    const dot_idx = filename.lastIndexOf(".");

    // < 0 means filename doesn't contain any "."
    // also skip index 0 because that would mean the basename is empty
    if (dot_idx > 1) {
        return filename.slice(dot_idx + 1);
    }

    return filename;
}

export function assert(condition: unknown, msg?: string | (() => string)): asserts condition {
    if (!condition) {
        let full_msg = "Assertion Error";

        if (msg) {
            full_msg += ": " + (msg instanceof Function ? msg() : msg);
        }

        throw new Error(full_msg);
    }
}

/**
 * Asserts that `value` is not null and not undefined.
 */
export function defined<T>(value: T | undefined | null, name: string): asserts value is T {
    assert(value != undefined, () => `${name} should not be null or undefined (was ${value}).`);
}

export function require_finite(value: unknown, name: string): asserts value is number {
    assert(Number.isFinite(value), () => `${name} should be a finite number (was ${value}).`);
}

export function require_number(value: unknown, name: string): asserts value is number {
    assert(typeof value === "number", () => `${name} should be a number (was ${value}).`);
}

export function require_integer(value: unknown, name: string): asserts value is number {
    assert(Number.isInteger(value), () => `${name} should be an integer (was ${value}).`);
}

export function require_non_negative_integer(
    value: unknown,
    name: string,
): asserts value is number {
    assert(
        Number.isInteger(value) && (value as any) >= 0,
        () => `${name} should be a non-negative integer (was ${value}).`,
    );
}

export function require_array<T>(value: readonly T[], name: string): void {
    assert(Array.isArray(value), () => `${name} should be an array (was ${value}).`);
}

export function number_to_hex_string(num: number, min_len: number = 8): string {
    return num.toString(16).padStart(min_len, "0");
}

export function browser_supports_webassembly(): boolean {
    return typeof window === "object" && typeof window.WebAssembly === "object";
}

/**
 * @returns true iff the given value implements PromiseLike.
 */
export function is_promise_like<T>(value?: T | PromiseLike<T>): value is PromiseLike<T> {
    return value != undefined && typeof (value as any).then === "function";
}

/**
 * @returns true iff the given value implements Promise.
 */
export function is_promise<T>(value?: T | Promise<T>): value is Promise<T> {
    return (
        value != undefined &&
        typeof (value as any).then === "function" &&
        typeof (value as any).catch === "function" &&
        typeof (value as any).finally === "function"
    );
}

export function try_finally<T>(f: () => T, after: () => void): T {
    let return_promise = false;

    try {
        const r = f();

        if (is_promise(r)) {
            return_promise = true;
            return (r.finally(() => after()) as unknown) as T;
        } else {
            return r;
        }
    } finally {
        if (!return_promise) {
            after();
        }
    }
}
