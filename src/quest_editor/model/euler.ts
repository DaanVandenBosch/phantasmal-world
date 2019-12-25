import { Euler, Quaternion } from "three";

/**
 * Creates an {@link Euler} object with the correct rotation order.
 */
export function euler(x: number, y: number, z: number): Euler {
    return new Euler(x, y, z, "ZXY");
}

/**
 * Creates an {@link Euler} object from a {@link Quaternion} with the correct rotation order.
 */
export function euler_from_quat(q: Quaternion): Euler {
    return new Euler().setFromQuaternion(q, "ZXY");
}
