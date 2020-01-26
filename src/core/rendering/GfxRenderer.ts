import { Renderer } from "./Renderer";
import { Scene } from "./Scene";
import { Camera } from "./Camera";
import { Gfx } from "./Gfx";
import { Mat4, Vec2, vec2_diff } from "../math/linear_algebra";
import { deg_to_rad } from "../math";

export abstract class GfxRenderer implements Renderer {
    private pointer_pos?: Vec2;
    /**
     * Is defined when an animation frame is scheduled.
     */
    private animation_frame?: number;

    protected projection_mat: Mat4 = Mat4.identity();

    abstract readonly gfx: Gfx;
    readonly scene = new Scene();
    readonly camera = new Camera();
    readonly canvas_element: HTMLCanvasElement = document.createElement("canvas");

    protected constructor(private readonly perspective_projection: boolean) {
        this.canvas_element.width = 800;
        this.canvas_element.height = 600;
        this.canvas_element.addEventListener("mousedown", this.mousedown);
        this.canvas_element.addEventListener("wheel", this.wheel, { passive: true });
    }

    dispose(): void {
        this.destroy_scene();
    }

    set_size(width: number, height: number): void {
        if (this.perspective_projection) {
            const fov = 75;
            const aspect = width / height;

            const n = 0.1;
            const f = 2000;
            const t = n * Math.tan(deg_to_rad(0.5 * fov));
            const b = -t;
            const r = aspect * t;
            const l = -r;

            // prettier-ignore
            this.projection_mat = Mat4.of(
                2*n / (r-l),           0,  (r+l) / (r-l),                0,
                          0, 2*n / (t-b),  (t+b) / (t-b),                0,
                          0,           0, -(f+n) / (f-n), -(2*f*n) / (f-n),
                          0,           0,             -1,                0,
            );
        } else {
            const w = width;
            const h = height;
            const n = -1000;
            const f = 1000;

            // prettier-ignore
            this.projection_mat = Mat4.of(
                2/w,   0,       0, 0,
                  0, 2/h,       0, 0,
                  0,   0, 2/(n-f), 0,
                  0,   0,       0, 1,
            );
        }

        this.schedule_render();
    }

    start_rendering(): void {
        this.schedule_render();
    }

    stop_rendering(): void {
        if (this.animation_frame != undefined) {
            cancelAnimationFrame(this.animation_frame);
        }

        this.animation_frame = undefined;
    }

    schedule_render = (): void => {
        if (this.animation_frame == undefined) {
            this.animation_frame = requestAnimationFrame(this.call_render);
        }
    };

    private call_render = (): void => {
        this.animation_frame = undefined;
        this.render();
    };

    protected abstract render(): void;

    /**
     * Destroys all GPU objects related to the scene and resets the scene.
     */
    destroy_scene(): void {
        this.scene.traverse(node => {
            node.mesh?.destroy(this.gfx);
            node.mesh?.texture?.destroy();
            node.mesh = undefined;
        }, undefined);

        this.scene.root_node.clear_children();
        this.scene.root_node.transform = Mat4.identity();
    }

    private mousedown = (evt: MouseEvent): void => {
        if (evt.buttons === 1) {
            this.pointer_pos = new Vec2(evt.clientX, evt.clientY);

            window.addEventListener("mousemove", this.mousemove);
            window.addEventListener("mouseup", this.mouseup);
        }
    };

    private mousemove = (evt: MouseEvent): void => {
        if (evt.buttons === 1) {
            const new_pos = new Vec2(evt.clientX, evt.clientY);
            const diff = vec2_diff(new_pos, this.pointer_pos!);
            this.camera.pan(-diff.x, diff.y, 0);
            this.pointer_pos = new_pos;
            this.schedule_render();
        }
    };

    private mouseup = (): void => {
        this.pointer_pos = undefined;

        window.removeEventListener("mousemove", this.mousemove);
        window.removeEventListener("mouseup", this.mouseup);
    };

    private wheel = (evt: WheelEvent): void => {
        if (this.perspective_projection) {
            if (evt.deltaY < 0) {
                this.camera.pan(0, 0, -10);
            } else {
                this.camera.pan(0, 0, 10);
            }
        } else {
            if (evt.deltaY < 0) {
                this.camera.zoom(1.1);
            } else {
                this.camera.zoom(0.9);
            }
        }

        this.schedule_render();
    };
}
