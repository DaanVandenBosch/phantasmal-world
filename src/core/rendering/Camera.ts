import { Mat4, Vec3 } from "../math/linear_algebra";

export class Camera {
    private readonly look_at: Vec3 = new Vec3(0, 0, 0);
    private x_rot: number = 0;
    private y_rot: number = 0;
    private z_rot: number = 0;
    private _zoom: number = 1;
    private readonly _mat4 = Mat4.identity();

    get mat4(): Mat4 {
        return this._mat4;
    }

    pan(x: number, y: number, z: number): this {
        this.look_at.x += x;
        this.look_at.y += y;
        this.look_at.z += z;
        this.update_matrix();
        return this;
    }

    /**
     * Increase (or decrease) zoom by a factor.
     */
    zoom(factor: number): this {
        this._zoom *= factor;
        this.look_at.x *= factor;
        this.look_at.y *= factor;
        this.look_at.z *= factor;
        this.update_matrix();
        return this;
    }

    reset(): this {
        this.look_at.x = 0;
        this.look_at.y = 0;
        this.look_at.z = 0;
        this.x_rot = 0;
        this.y_rot = 0;
        this.z_rot = 0;
        this._zoom = 1;
        this.update_matrix();
        return this;
    }

    private update_matrix(): void {
        this._mat4.data[12] = -this.look_at.x;
        this._mat4.data[13] = -this.look_at.y;
        this._mat4.data[14] = -this.look_at.z;
        this._mat4.data[0] = this._zoom;
        this._mat4.data[5] = this._zoom;
        this._mat4.data[10] = this._zoom;
    }
}
