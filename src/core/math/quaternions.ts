export enum EulerOrder {
    ZXY,
    ZYX,
}

export class Quat {
    /**
     * Creates a quaternion from Euler angles.
     *
     * @param x - Rotation around the x-axis in radians.
     * @param y - Rotation around the y-axis in radians.
     * @param z - Rotation around the z-axis in radians.
     * @param order - Order in which rotations are applied.
     */
    static euler_angles(x: number, y: number, z: number, order: EulerOrder): Quat {
        const cos_x = Math.cos(x * 0.5);
        const sin_x = Math.sin(x * 0.5);
        const cos_y = Math.cos(y * 0.5);
        const sin_y = Math.sin(y * 0.5);
        const cos_z = Math.cos(z * 0.5);
        const sin_z = Math.sin(z * 0.5);

        switch (order) {
            case EulerOrder.ZXY:
                return new Quat(
                    cos_x * cos_y * cos_z - sin_x * sin_y * sin_z,
                    sin_x * cos_y * cos_z - cos_x * sin_y * sin_z,
                    cos_x * sin_y * cos_z + sin_x * cos_y * sin_z,
                    cos_x * cos_y * sin_z + sin_x * sin_y * cos_z,
                );
            case EulerOrder.ZYX:
                return new Quat(
                    cos_x * cos_y * cos_z + sin_x * sin_y * sin_z,
                    sin_x * cos_y * cos_z - cos_x * sin_y * sin_z,
                    cos_x * sin_y * cos_z + sin_x * cos_y * sin_z,
                    cos_x * cos_y * sin_z - sin_x * sin_y * cos_z,
                );
        }
    }

    constructor(public w: number, public x: number, public y: number, public z: number) {}

    conjugate(): void {
        this.x *= -1;
        this.y *= -1;
        this.z *= -1;
    }
}

export function quat_product(p: Quat, q: Quat): Quat {
    return new Quat(
        p.w * q.w - p.x * q.x - p.y * q.y - p.z * q.z,
        p.w * q.x + p.x * q.w + p.y * q.z - p.z * q.y,
        p.w * q.y - p.x * q.z + p.y * q.w + p.z * q.x,
        p.w * q.z + p.x * q.y - p.y * q.x + p.z * q.w,
    );
}
