import CameraControls from "camera-controls";
import * as THREE from "three";
import {
    Clock,
    Color,
    Group,
    HemisphereLight,
    OrthographicCamera,
    PerspectiveCamera,
    Scene,
    Vector2,
    Vector3,
} from "three";
import { Disposable } from "../observable/Disposable";
import { Renderer } from "./Renderer";

CameraControls.install({
    // Hack to make panning and orbiting work the way we want.
    THREE: {
        ...THREE,
        MOUSE: { ...THREE.MOUSE, LEFT: THREE.MOUSE.RIGHT, RIGHT: THREE.MOUSE.LEFT },
    },
});

export interface DisposableThreeRenderer extends THREE.Renderer, Disposable {}

/**
 * Uses THREE.js for rendering.
 */
export abstract class ThreeRenderer implements Renderer {
    private _debug = false;

    get debug(): boolean {
        return this._debug;
    }

    set debug(debug: boolean) {
        this._debug = debug;
    }

    abstract readonly camera: PerspectiveCamera | OrthographicCamera;
    readonly controls!: CameraControls;
    readonly scene = new Scene();
    readonly light_holder = new Group();

    private readonly renderer: DisposableThreeRenderer;
    private render_scheduled = false;
    private animation_frame_handle?: number = undefined;
    private readonly light = new HemisphereLight(0xffffff, 0x505050, 1.0);
    private readonly controls_clock = new Clock();
    private readonly size = new Vector2(0, 0);

    protected constructor(three_renderer: DisposableThreeRenderer) {
        this.renderer = three_renderer;
        this.renderer.domElement.tabIndex = 0;
        this.renderer.domElement.addEventListener("mousedown", this.on_mouse_down);
        this.renderer.domElement.style.outline = "none";

        this.scene.background = new Color(0x181818);
        this.light_holder.add(this.light);
        this.scene.add(this.light_holder);
    }

    get canvas_element(): HTMLCanvasElement {
        return this.renderer.domElement;
    }

    set_size(width: number, height: number): void {
        this.size.set(width, height);
        this.renderer.setSize(width, height);
        this.schedule_render();
    }

    pointer_pos_to_device_coords(pos: Vector2): void {
        pos.set((pos.x / this.size.width) * 2 - 1, (pos.y / this.size.height) * -2 + 1);
    }

    start_rendering(): void {
        if (this.animation_frame_handle == undefined) {
            this.schedule_render();
            this.animation_frame_handle = requestAnimationFrame(this.call_render);
        }
    }

    stop_rendering(): void {
        if (this.animation_frame_handle != undefined) {
            cancelAnimationFrame(this.animation_frame_handle);
            this.animation_frame_handle = undefined;
        }
    }

    schedule_render = (): void => {
        this.render_scheduled = true;
    };

    reset_camera(position: Vector3, look_at: Vector3): void {
        this.controls.setLookAt(
            position.x,
            position.y,
            position.z,
            look_at.x,
            look_at.y,
            look_at.z,
        );
    }

    dispose(): void {
        this.renderer.dispose();
        this.controls.dispose();
    }

    protected init_camera_controls(): void {
        (this.controls as CameraControls) = new CameraControls(
            this.camera,
            this.renderer.domElement,
        );
        this.controls.dampingFactor = 1;
        this.controls.draggingDampingFactor = 1;
    }

    protected render(): void {
        this.renderer.render(this.scene, this.camera);
    }

    private on_mouse_down = (e: Event): void => {
        if (e.currentTarget) (e.currentTarget as HTMLElement).focus();
    };

    private call_render = (): void => {
        const controls_updated = this.controls.update(this.controls_clock.getDelta());
        const should_render = this.render_scheduled || controls_updated;

        this.render_scheduled = false;

        if (should_render) {
            this.render();
        }

        this.animation_frame_handle = requestAnimationFrame(this.call_render);
    };
}
