import { Renderer } from "./Renderer";
import { VertexFormat } from "./VertexFormat";
import { MeshBuilder } from "./MeshBuilder";
import { Scene } from "./Scene";
import { Camera } from "./Camera";
import { Gfx } from "./Gfx";
import { Mat4, Vec2, vec2_diff } from "../math";

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

    protected constructor() {
        this.canvas_element.width = 800;
        this.canvas_element.height = 600;
        this.canvas_element.addEventListener("mousedown", this.mousedown);
        this.canvas_element.addEventListener("wheel", this.wheel, { passive: true });
    }

    dispose(): void {
        this.scene.destroy();
    }

    set_size(width: number, height: number): void {
        // prettier-ignore
        this.projection_mat = Mat4.of(
            2/width, 0,        0,    0,
            0,       2/height, 0,    0,
            0,       0,        2/10, 0,
            0,       0,        0,    1,
        );

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

    mesh_builder(vertex_format: VertexFormat): MeshBuilder {
        return new MeshBuilder(this.gfx, vertex_format);
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
        if (evt.deltaY < 0) {
            this.camera.zoom(1.1);
        } else {
            this.camera.zoom(0.9);
        }

        this.schedule_render();
    };
}
