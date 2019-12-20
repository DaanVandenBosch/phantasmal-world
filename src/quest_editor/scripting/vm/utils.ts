export type BinaryNumericOperation = (a: number, b: number) => number;

export const numeric_ops: Record<
    "add" | "sub" | "mul" | "div" | "idiv" | "mod" | "and" | "or" | "xor" | "shl" | "shr",
    BinaryNumericOperation
> = {
    add: (a, b) => a + b,
    sub: (a, b) => a - b,
    mul: (a, b) => a * b,
    div: (a, b) => a / b,
    idiv: (a, b) => Math.floor(a / b),
    mod: (a, b) => a % b,
    and: (a, b) => a & b,
    or: (a, b) => a | b,
    xor: (a, b) => a ^ b,
    shl: (a, b) => a << b,
    shr: (a, b) => a >>> b,
};

export type ComparisonOperation = (a: number, b: number) => boolean;

/**
 * Short-circuiting fold.
 */
export function andfold<T, A>(
    fn: (acc: A, cur: T) => A | undefined,
    init: A,
    lst: T[],
): A | undefined {
    let acc = init;

    for (const item of lst) {
        const new_val = fn(acc, item);

        if (new_val === undefined) {
            return undefined;
        } else {
            acc = new_val;
        }
    }

    return acc;
}

/**
 * Short-circuiting reduce.
 */
export function andreduce<T>(fn: (acc: T, cur: T) => T | undefined, lst: T[]): T | undefined {
    return andfold(fn, lst[0], lst.slice(1));
}

/**
 * Applies the given arguments to the given function.
 * Returns the second argument if the function returns a truthy value, else undefined.
 */
export function andsecond<T>(fn: (first: T, second: T) => any, first: T, second: T): T | undefined {
    if (fn(first, second)) {
        return second;
    }
    return undefined;
}

export function rest<T>(lst: T[]): T[] {
    return lst.slice(1);
}
