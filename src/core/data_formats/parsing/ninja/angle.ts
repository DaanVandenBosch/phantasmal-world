const ANGLE_TO_RAD = (2 * Math.PI) / 0x10000;
const RAD_TO_ANGLE = 0x10000 / (2 * Math.PI);

export function angle_to_rad(angle: number): number {
    return angle * ANGLE_TO_RAD;
}

export function rad_to_angle(rad: number): number {
    return Math.round(rad * RAD_TO_ANGLE);
}
