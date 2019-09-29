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
