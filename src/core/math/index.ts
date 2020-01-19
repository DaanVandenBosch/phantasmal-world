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

export class Vec2 {
    constructor(public x: number, public y: number) {}
}

export function vec2_diff(v: Vec2, w: Vec2): Vec2 {
    return new Vec2(v.x - w.x, v.y - w.y);
}

export class Vec3 {
    constructor(public x: number, public y: number, public z: number) {}
}

export class Mat4 {
    static of(...values: readonly number[]): Mat4 {
        return new Mat4(new Float32Array(values));
    }

    static identity(): Mat4 {
        // prettier-ignore
        return Mat4.of(
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

export function mat4_product(a: Mat4, b: Mat4): Mat4 {
    const c = new Mat4(new Float32Array(16));
    mat4_product_into_array(c.data, a, b);
    return c;
}

export function mat4_multiply(a: Mat4, b: Mat4): void {
    const array = new Float32Array(16);
    mat4_product_into_array(array, a, b);
    a.data.set(array);
}

function mat4_product_into_array(array: Float32Array, a: Mat4, b: Mat4): void {
    for (let i = 0; i < 4; i++) {
        for (let j = 0; j < 4; j++) {
            for (let k = 0; k < 4; k++) {
                array[i * 4 + j] += a.data[i * 4 + k] * b.data[k * 4 + j];
            }
        }
    }
}

export class Quat {
    constructor(public x: number, public y: number, public z: number, public w: number) {}
}
