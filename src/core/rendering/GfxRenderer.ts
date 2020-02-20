import { Renderer } from "./Renderer";
import { Scene } from "./Scene";
import { Camera, Projection } from "./Camera";
import { Gfx } from "./Gfx";
import { Mat4, Vec2, vec2_diff } from "../math/linear_algebra";

export abstract class GfxRenderer implements Renderer {
    private pointer_pos?: Vec2;
    /**
     * Is defined when an animation frame is scheduled.
     */
    private animation_frame?: number;

    protected width: number = 800;
    protected height: number = 600;

    abstract readonly gfx: Gfx;
    readonly scene = new Scene();
    readonly camera: Camera;
    readonly canvas_element: HTMLCanvasElement = document.createElement("canvas");

    protected constructor(projection: Projection) {
        this.canvas_element.width = this.width;
        this.canvas_element.height = this.height;
        this.canvas_element.addEventListener("mousedown", this.mousedown);
        this.canvas_element.addEventListener("wheel", this.wheel, { passive: true });

        this.camera = new Camera(this.width, this.height, projection);
    }

    dispose(): void {
        this.destroy_scene();
    }

    set_size(width: number, height: number): void {
        this.width = width;
        this.height = height;
        this.camera.set_viewport(width, height);
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
        this.pointer_pos = new Vec2(evt.clientX, evt.clientY);

        window.addEventListener("mousemove", this.mousemove);
        window.addEventListener("mouseup", this.mouseup);
        window.addEventListener("contextmenu", this.contextmenu);
    };

    private mousemove = (evt: MouseEvent): void => {
        const new_pos = new Vec2(evt.clientX, evt.clientY);
        const diff = vec2_diff(new_pos, this.pointer_pos!);

        if (evt.buttons === 1) {
            this.camera.pan(-diff.x, diff.y, 0);
        } else if (evt.buttons === 2) {
            this.camera.rotate(-diff.x / (20 * Math.PI), -diff.y / (20 * Math.PI));
        }

        this.pointer_pos = new_pos;
        this.schedule_render();
    };

    private mouseup = (evt: MouseEvent): void => {
        evt.preventDefault();

        this.pointer_pos = undefined;

        window.removeEventListener("mousemove", this.mousemove);
        window.removeEventListener("mouseup", this.mouseup);
    };

    private wheel = (evt: WheelEvent): void => {
        switch (this.camera.projection) {
            case Projection.Orthographic:
                if (evt.deltaY < 0) {
                    this.camera.zoom(1.1);
                } else {
                    this.camera.zoom(0.9);
                }
                break;

            case Projection.Perspective:
                if (evt.deltaY < 0) {
                    this.camera.pan(0, 0, -2);
                } else {
                    this.camera.pan(0, 0, 2);
                }
                break;
        }

        this.schedule_render();
    };

    private contextmenu = (evt: Event): void => {
        evt.preventDefault();
        window.removeEventListener("contextmenu", this.contextmenu);
    };
}
