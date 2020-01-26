import { assert } from "../util";
import { Quat } from "./quaternions";

export class Vec2 {
    constructor(public x: number, public y: number) {}

    get u(): number {
        return this.x;
    }

    get v(): number {
        return this.y;
    }
}

export function vec2_diff(v: Vec2, w: Vec2): Vec2 {
    return new Vec2(v.x - w.x, v.y - w.y);
}

export class Vec3 {
    constructor(public x: number, public y: number, public z: number) {}
}

/**
 * Stores data in column-major order.
 */
export class Mat3 {
    // prettier-ignore
    static of(
        m00: number, m01: number, m02: number,
        m10: number, m11: number, m12: number,
        m20: number, m21: number, m22: number,
    ): Mat3 {
        return new Mat3(new Float32Array([
            m00, m10, m20,
            m01, m11, m21,
            m02, m12, m22,
        ]));
    }

    static identity(): Mat3 {
        // prettier-ignore
        return Mat3.of(
            1, 0, 0,
            0, 1, 0,
            0, 0, 1,
        )
    }

    constructor(readonly data: Float32Array) {
        assert(data.length === 9, "data should be of length 9.");
    }

    get(i: number, j: number): number {
        return this.data[i + j * 3];
    }

    set(i: number, j: number, value: number): void {
        this.data[i + j * 3] = value;
    }

    /**
     * @returns a copy of this matrix.
     */
    clone(): Mat3 {
        return new Mat3(new Float32Array(this.data));
    }

    /**
     * Transposes this matrix in-place.
     */
    transpose(): void {
        let tmp: number;
        const m = this.data;

        tmp = m[1];
        m[1] = m[3];
        m[3] = tmp;

        tmp = m[2];
        m[2] = m[6];
        m[6] = tmp;

        tmp = m[5];
        m[5] = m[7];
        m[7] = tmp;
    }

    /**
     * Computes the inverse of this matrix and returns it as a new {@link Mat3}.
     *
     * @returns the inverse of this matrix.
     */
    inverse(): Mat3 {
        const m = this.clone();
        m.invert();
        return m;
    }

    /**
     * Computes the inverse of this matrix in-place. Will revert to identity if this matrix is
     * degenerate.
     */
    invert(): void {
        const n11 = this.data[0];
        const n21 = this.data[1];
        const n31 = this.data[2];
        const n12 = this.data[3];
        const n22 = this.data[4];
        const n32 = this.data[5];
        const n13 = this.data[6];
        const n23 = this.data[7];
        const n33 = this.data[8];
        const t11 = n33 * n22 - n32 * n23;
        const t12 = n32 * n13 - n33 * n12;
        const t13 = n23 * n12 - n22 * n13;
        const det = n11 * t11 + n21 * t12 + n31 * t13;

        if (det === 0) {
            // Revert to identity if matrix is degenerate.
            this.data[0] = 1;
            this.data[1] = 0;
            this.data[2] = 0;

            this.data[3] = 0;
            this.data[4] = 1;
            this.data[5] = 0;

            this.data[6] = 0;
            this.data[7] = 0;
            this.data[8] = 1;

            return;
        }

        const det_inv = 1 / det;

        this.data[0] = t11 * det_inv;
        this.data[1] = (n31 * n23 - n33 * n21) * det_inv;
        this.data[2] = (n32 * n21 - n31 * n22) * det_inv;

        this.data[3] = t12 * det_inv;
        this.data[4] = (n33 * n11 - n31 * n13) * det_inv;
        this.data[5] = (n31 * n12 - n32 * n11) * det_inv;

        this.data[6] = t13 * det_inv;
        this.data[7] = (n21 * n13 - n23 * n11) * det_inv;
        this.data[8] = (n22 * n11 - n21 * n12) * det_inv;
    }
}

export function mat3_product(a: Mat3, b: Mat3): Mat3 {
    const c = new Mat3(new Float32Array(9));
    mat3_product_into_array(c.data, a, b);
    return c;
}

export function mat3_multiply(a: Mat3, b: Mat3): void {
    const array = new Float32Array(9);
    mat3_product_into_array(array, a, b);
    a.data.set(array);
}

function mat3_product_into_array(array: Float32Array, a: Mat3, b: Mat3): void {
    for (let i = 0; i < 3; i++) {
        for (let j = 0; j < 3; j++) {
            for (let k = 0; k < 3; k++) {
                array[i + j * 3] += a.data[i + k * 3] * b.data[k + j * 3];
            }
        }
    }
}

/**
 * Computes the product of `m` and `v` and stores the result in `v`.
 */
export function mat3_vec3_multiply(m: Mat3, v: Vec3): void {
    const x = m.get(0, 0) * v.x + m.get(0, 1) * v.y + m.get(0, 2) * v.z;
    const y = m.get(1, 0) * v.x + m.get(1, 1) * v.y + m.get(1, 2) * v.z;
    const z = m.get(2, 0) * v.x + m.get(2, 1) * v.y + m.get(2, 2) * v.z;
    v.x = x;
    v.y = y;
    v.z = z;
}

/**
 * Stores data in column-major order.
 */
export class Mat4 {
    // prettier-ignore
    static of(
        m00: number, m01: number, m02: number, m03: number,
        m10: number, m11: number, m12: number, m13: number,
        m20: number, m21: number, m22: number, m23: number,
        m30: number, m31: number, m32: number, m33: number,
    ): Mat4 {
        return new Mat4(new Float32Array([
            m00, m10, m20, m30,
            m01, m11, m21, m31,
            m02, m12, m22, m32,
            m03, m13, m23, m33,
        ]));
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

    static translation(x: number, y: number, z: number): Mat4 {
        // prettier-ignore
        return Mat4.of(
            1, 0, 0, x,
            0, 1, 0, y,
            0, 0, 1, z,
            0, 0, 0, 1,
        );
    }

    static scale(x: number, y: number, z: number): Mat4 {
        // prettier-ignore
        return Mat4.of(
            x, 0, 0, 1,
            0, y, 0, 1,
            0, 0, z, 1,
            0, 0, 0, 1,
        );
    }

    static compose(translation: Vec3, rotation: Quat, scale: Vec3): Mat4 {
        const w = rotation.w;
        const x = rotation.x;
        const y = rotation.y;
        const z = rotation.z;
        const x2 = x + x;
        const y2 = y + y;
        const z2 = z + z;
        const xx = x * x2;
        const xy = x * y2;
        const xz = x * z2;
        const yy = y * y2;
        const yz = y * z2;
        const zz = z * z2;
        const wx = w * x2;
        const wy = w * y2;
        const wz = w * z2;

        const sx = scale.x;
        const sy = scale.y;
        const sz = scale.z;

        // prettier-ignore
        return Mat4.of(
            (1 - (yy + zz)) * sx, (xy - wz) * sy,       (xz + wy) * sz,       translation.x,
            (xy + wz) * sx,       (1 - (xx + zz)) * sy, (yz - wx) * sz,       translation.y,
            (xz - wy) * sx,       (yz + wx) * sy,       (1 - (xx + yy)) * sz, translation.z,
            0,                    0,                    0,                    1
        )
    }

    constructor(readonly data: Float32Array) {
        assert(data.length === 16, "data should be of length 16.");
    }

    get(i: number, j: number): number {
        return this.data[i + j * 4];
    }

    set(i: number, j: number, value: number): void {
        this.data[i + j * 4] = value;
    }

    clone(): Mat4 {
        return new Mat4(new Float32Array(this.data));
    }

    /**
     * Computes a 3 x 3 surface normal transformation matrix.
     */
    normal_mat3(): Mat3 {
        // prettier-ignore
        const m = Mat3.of(
            this.data[0], this.data[4], this.data[8],
            this.data[1], this.data[5], this.data[9],
            this.data[2], this.data[6], this.data[10],
        );
        m.invert();
        m.transpose();
        return m;
    }
}

export function mat4_product(a: Mat4, b: Mat4): Mat4 {
    const c = new Mat4(new Float32Array(16));
    mat4_product_into_array(c.data, a, b);
    return c;
}

/**
 * Computes the product of `a` and `b` and stores the result in `a`.
 */
export function mat4_multiply(a: Mat4, b: Mat4): void {
    const array = new Float32Array(16);
    mat4_product_into_array(array, a, b);
    a.data.set(array);
}

function mat4_product_into_array(array: Float32Array, a: Mat4, b: Mat4): void {
    for (let i = 0; i < 4; i++) {
        for (let j = 0; j < 4; j++) {
            for (let k = 0; k < 4; k++) {
                array[i + j * 4] += a.data[i + k * 4] * b.data[k + j * 4];
            }
        }
    }
}

/**
 * Computes the product of `m` and `v` and stores the result in `v`. Assumes `m` is affine.
 */
export function mat4_vec3_multiply(m: Mat4, v: Vec3): void {
    const x = m.get(0, 0) * v.x + m.get(0, 1) * v.y + m.get(0, 2) * v.z + m.get(0, 3);
    const y = m.get(1, 0) * v.x + m.get(1, 1) * v.y + m.get(1, 2) * v.z + m.get(1, 3);
    const z = m.get(2, 0) * v.x + m.get(2, 1) * v.y + m.get(2, 2) * v.z + m.get(2, 3);
    v.x = x;
    v.y = y;
    v.z = z;
}
