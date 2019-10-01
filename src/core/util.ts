export function arrays_equal<T>(
    a: T[],
    b: T[],
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
