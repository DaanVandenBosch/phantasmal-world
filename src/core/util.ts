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

export function array_buffers_equal(a: ArrayBuffer, b: ArrayBuffer): boolean {
    if (a.byteLength !== b.byteLength) return false;

    const a_arr = new Uint8Array(a);
    const b_arr = new Uint8Array(b);

    for (let i = 0; i < a_arr.length; i++) {
        if (a_arr[i] !== b_arr[i]) return false;
    }

    return true;
}

/**
 * Returns the given filename without the file extension.
 */
export function basename(filename: string): string {
    const dot_idx = filename.lastIndexOf(".");

    // < 0 means filenames doesn't contain any "."
    // also skip index 0 because that would mean the basename is empty
    if (dot_idx > 1) {
        return filename.slice(0, dot_idx);
    }

    return filename;
}

export function defined<T>(value: T | undefined): asserts value is T {
    if (value === undefined) {
        throw new Error("Assertion Error: value is undefined.");
    }
}

export function assert(condition: any, msg?: string): asserts condition {
    if (!condition) {
        let full_msg = "Assertion Error";
        
        if (msg) {
            full_msg += ": " + msg;
        }

        throw new Error(full_msg);
    }
}
