import { Mat4, Vec3 } from "../math";
import { Mat4Transform, Transform } from "./Transform";

export class Camera {
    private readonly look_at: Vec3 = new Vec3(0, 0, 0);
    private x_rot: number = 0;
    private y_rot: number = 0;
    private z_rot: number = 0;
    private _zoom: number = 1;
    private readonly _transform = new Mat4Transform(Mat4.identity());

    get transform(): Transform {
        return this._transform;
    }

    pan(x: number, y: number, z: number): this {
        this.look_at.x += x;
        this.look_at.y += y;
        this.look_at.z += z;
        this.update_transform();
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
        this.update_transform();
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
        this.update_transform();
        return this;
    }

    private update_transform(): void {
        this._transform.data[3] = -this.look_at.x;
        this._transform.data[7] = -this.look_at.y;
        this._transform.data[11] = -this.look_at.z;
        this._transform.data[0] = this._zoom;
        this._transform.data[5] = this._zoom;
        this._transform.data[10] = this._zoom;
    }
}
