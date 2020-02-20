import { Mat4, Vec3, vec3_cross, vec3_dot, vec3_sub } from "../math/linear_algebra";
import { clamp, deg_to_rad } from "../math";

export enum Projection {
    Orthographic,
    Perspective,
}

export class Camera {
    /**
     * Only applicable in perspective mode.
     */
    private readonly fov = deg_to_rad(75);
    private readonly target: Vec3 = new Vec3(0, 0, 0);

    // Spherical coordinates.
    private radius = 0;
    private azimuth = 0;
    private polar = Math.PI / 2;

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
                    (3 * this.radius * Math.tan(0.5 * this.effective_fov)) / this.viewport_width;
                break;
        }

        x *= pan_factor;
        y *= pan_factor;

        this.target.x += x;
        this.target.y += y;

        this.radius += z;

        this.update_matrix();
        return this;
    }

    rotate(azimuth: number, polar: number): this {
        this.azimuth += azimuth;
        const max_pole_dist = Math.PI / 1800; // tenth of a degree.
        this.polar = clamp(this.polar + polar, max_pole_dist, Math.PI - max_pole_dist);
        this.update_matrix();
        return this;
    }

    /**
     * Increase (or decrease) zoom by a factor.
     */
    zoom(factor: number): this {
        this._zoom *= factor;
        this.target.x *= factor;
        this.target.y *= factor;
        this.target.z *= factor;
        this.update_matrix();
        return this;
    }

    reset(): this {
        this.target.x = 0;
        this.target.y = 0;
        this.target.z = 0;
        this._zoom = 1;
        this.update_matrix();
        return this;
    }

    private update_matrix(): void {
        // Convert spherical coordinates to cartesian coordinates.
        const radius_sin_polar = this.radius * Math.sin(this.polar);
        const camera_pos = new Vec3(
            this.target.x + radius_sin_polar * Math.sin(this.azimuth),
            this.target.y + this.radius * Math.cos(this.polar),
            this.target.z + radius_sin_polar * Math.cos(this.azimuth),
        );

        // Compute forward (z-axis), right (x-axis) and up (y-axis) vectors.
        const forward = vec3_sub(camera_pos, this.target);
        forward.normalize();

        const right = vec3_cross(new Vec3(0, 1, 0), forward);
        right.normalize();

        const up = vec3_cross(forward, right);

        const zoom = this._zoom;

        // prettier-ignore
        this.view_matrix.set_all(
            right.x * zoom,   right.y,   right.z, -vec3_dot(  right, camera_pos),
                 up.x,      up.y* zoom,      up.z, -vec3_dot(     up, camera_pos),
            forward.x, forward.y, forward.z* zoom, -vec3_dot(forward, camera_pos),
                    0,         0,         0,                              1,
        );
    }
}
