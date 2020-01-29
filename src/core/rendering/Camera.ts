import { Mat4, Vec3, vec3_dist } from "../math/linear_algebra";
import { deg_to_rad } from "../math";

export enum Projection {
    Orthographic,
    Perspective,
}

export class Camera {
    // Only applicable in perspective mode.
    private readonly fov = deg_to_rad(75);
    private readonly position: Vec3 = new Vec3(0, 0, 0);
    private readonly look_at: Vec3 = new Vec3(0, 0, 0);
    private x_rot: number = 0;
    private y_rot: number = 0;
    private z_rot: number = 0;
    private _zoom: number = 1;

    readonly view_mat4 = Mat4.identity();
    readonly projection_mat4 = Mat4.identity();

    /**
     * Effective field of view in radians. Only applicable in perspective mode.
     */
    get effective_fov(): number {
        return 2 * Math.atan(Math.tan(0.5 * this.fov) / this._zoom);
    }

    constructor(
        private viewport_width: number,
        private viewport_height: number,
        readonly projection: Projection,
    ) {
        this.set_viewport(viewport_width, viewport_height);
    }

    set_viewport(width: number, height: number): void {
        this.viewport_width = width;
        this.viewport_height = height;

        switch (this.projection) {
            case Projection.Orthographic:
                {
                    const w = width;
                    const h = height;
                    const n = -1000;
                    const f = 1000;

                    // prettier-ignore
                    this.projection_mat4.set_all(
                        2/w,   0,       0, 0,
                          0, 2/h,       0, 0,
                          0,   0, 2/(n-f), 0,
                          0,   0,       0, 1,
                    );
                }
                break;

            case Projection.Perspective:
                {
                    const aspect = width / height;

                    const n = 0.1;
                    const f = 2000;
                    const t = n * Math.tan(0.5 * this.fov);
                    const b = -t;
                    const r = aspect * t;
                    const l = -r;

                    // prettier-ignore
                    this.projection_mat4.set_all(
                        2*n / (r-l),           0, (l+r) / (l-r),               0,
                                  0, 2*n / (t-b), (b+t) / (b-t),               0,
                                  0,           0, (f+n) / (n-f), (2*f*n) / (f-n),
                                  0,           0,             1,               0,
                    );
                }
                break;
        }
    }

    pan(x: number, y: number, z: number): this {
        const pan_factor =
            (3 * vec3_dist(this.position, this.look_at) * Math.tan(0.5 * this.effective_fov)) /
            this.viewport_width;

        x *= pan_factor;
        y *= pan_factor;

        this.position.x += x;
        this.position.y += y;
        this.position.z += z;
        this.look_at.x += x;
        this.look_at.y += y;

        this.update_matrix();
        return this;
    }

    /**
     * Increase (or decrease) zoom by a factor.
     */
    zoom(factor: number): this {
        this._zoom *= factor;
        this.position.x *= factor;
        this.position.y *= factor;
        this.position.z *= factor;
        this.look_at.x *= factor;
        this.look_at.y *= factor;
        this.look_at.z *= factor;
        this.update_matrix();
        return this;
    }

    reset(): this {
        this.position.x = 0;
        this.position.y = 0;
        this.position.z = 0;
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
        this.view_mat4.data[12] = this._zoom * -this.position.x;
        this.view_mat4.data[13] = this._zoom * -this.position.y;
        this.view_mat4.data[14] = this._zoom * -this.position.z;
        this.view_mat4.data[0] = this._zoom;
        this.view_mat4.data[5] = this._zoom;
        this.view_mat4.data[10] = this._zoom;
    }
}
