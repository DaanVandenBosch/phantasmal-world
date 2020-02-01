import { Mat4, Vec3, vec3_dist } from "../math/linear_algebra";
import { deg_to_rad } from "../math";

export enum Projection {
    Orthographic,
    Perspective,
}

export class Camera {
    /**
     * Only applicable in perspective mode.
     */
    private readonly fov = deg_to_rad(75);
    private readonly position: Vec3 = new Vec3(0, 0, 0);
    private readonly look_at: Vec3 = new Vec3(0, 0, 0);
    private x_rot: number = 0;
    private y_rot: number = 0;
    private z_rot: number = 0;
    private _zoom: number = 1;

    /**
     * Effective field of view in radians. Only applicable in perspective mode.
     */
    private get effective_fov(): number {
        return 2 * Math.atan(Math.tan(0.5 * this.fov) / this._zoom);
    }

    readonly view_matrix = Mat4.identity();
    readonly projection_matrix = Mat4.identity();

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
                    const n = 0;
                    const f = 100;

                    // prettier-ignore
                    this.projection_matrix.set_all(
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

                    const n /* near */ = 0.1;
                    const f /* far */ = 2000;
                    const t /* top */ = (n * Math.tan(0.5 * this.fov)) / this._zoom;
                    const h /* height */ = 2 * t;
                    const w /* width */ = 2 * aspect * t;

                    // prettier-ignore
                    this.projection_matrix.set_all(
                        2*n / w,       0,             0,             0,
                              0, 2*n / h,             0,             0,
                              0,       0, (n+f) / (n-f), 2*n*f / (n-f),
                              0,       0,            -1,             0,
                    );
                }
                break;
        }
    }

    pan(x: number, y: number, z: number): this {
        let pan_factor: number;

        switch (this.projection) {
            case Projection.Orthographic:
                pan_factor = 1;
                break;

            case Projection.Perspective:
                pan_factor =
                    (3 *
                        vec3_dist(this.position, this.look_at) *
                        Math.tan(0.5 * this.effective_fov)) /
                    this.viewport_width;
                break;
        }

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
        this.view_matrix.data[12] = -this.position.x;
        this.view_matrix.data[13] = -this.position.y;
        this.view_matrix.data[14] = -this.position.z;
        this.view_matrix.data[0] = this._zoom;
        this.view_matrix.data[5] = this._zoom;
        this.view_matrix.data[10] = this._zoom;
    }
}
