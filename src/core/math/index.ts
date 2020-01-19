import { assert } from "../util";

const TO_DEG = 180 / Math.PI;
const TO_RAD = 1 / TO_DEG;

/**
 * Converts radians to degrees.
 */
export function rad_to_deg(rad: number): number {
    return rad * TO_DEG;
}

/**
 * Converts degrees to radians.
 */
export function deg_to_rad(deg: number): number {
    return deg * TO_RAD;
}

/**
 * @returns the floored modulus of its arguments. The computed value will have the same sign as the
 * `divisor`.
 */
export function floor_mod(dividend: number, divisor: number): number {
    return ((dividend % divisor) + divisor) % divisor;
}

export class Matrix4 {
    static of(...values: readonly number[]): Matrix4 {
        return new Matrix4(new Float32Array(values));
    }

    static identity(): Matrix4 {
        // prettier-ignore
        return Matrix4.of(
            1, 0, 0, 0,
            0, 1, 0, 0,
            0, 0, 1, 0,
            0, 0, 0, 1,
        )
    }

    constructor(readonly data: Float32Array) {
        assert(data.length === 16, "values should be of length 16.");
    }
}

export function matrix4_product(a: Matrix4, b: Matrix4): Matrix4 {
    const array = new Float32Array(16);

    for (let i = 0; i < 4; i++) {
        for (let j = 0; j < 4; j++) {
            for (let k = 0; k < 4; k++) {
                array[i * 4 + j] += a.data[i * 4 + k] * b.data[k * 4 + j];
            }
        }
    }

    return new Matrix4(array);
}
