import CameraControls from "camera-controls";
import * as THREE from "three";
import {
    Camera,
    Clock,
    Color,
    Group,
    HemisphereLight,
    OrthographicCamera,
    PerspectiveCamera,
    Scene,
    Vector2,
    Vector3,
    WebGLRenderer,
} from "three";
import { Disposable } from "../observable/Disposable";

CameraControls.install({
    // Hack to make panning and orbiting work the way we want.
    THREE: {
        ...THREE,
        MOUSE: { ...THREE.MOUSE, LEFT: THREE.MOUSE.RIGHT, RIGHT: THREE.MOUSE.LEFT },
    },
});

export abstract class Renderer implements Disposable {
    protected _debug = false;

    get debug(): boolean {
        return this._debug;
    }

    set debug(debug: boolean) {
        this._debug = debug;
    }

    readonly camera: Camera;
    readonly controls: CameraControls;
    readonly scene = new Scene();
    readonly light_holder = new Group();

    private renderer = new WebGLRenderer({ antialias: true });
    private render_scheduled = false;
    private render_stop_scheduled = false;
    private light = new HemisphereLight(0xffffff, 0x505050, 1.2);
    private controls_clock = new Clock();

    protected constructor(camera: PerspectiveCamera | OrthographicCamera) {
        this.camera = camera;

        this.dom_element.tabIndex = 0;
        this.dom_element.addEventListener("mousedown", this.on_mouse_down);
        this.dom_element.style.outline = "none";

        this.controls = new CameraControls(camera, this.renderer.domElement);
        this.controls.dampingFactor = 1;
        this.controls.draggingDampingFactor = 1;

        this.scene.background = new Color(0x181818);
        this.light_holder.add(this.light);
        this.scene.add(this.light_holder);

        this.renderer.setPixelRatio(window.devicePixelRatio);
    }

    get dom_element(): HTMLElement {
        return this.renderer.domElement;
    }

    set_size(width: number, height: number): void {
        this.renderer.setSize(width, height);
        this.schedule_render();
    }

    pointer_pos_to_device_coords(e: MouseEvent): Vector2 {
        const coords = this.renderer.getSize(new Vector2());
        coords.width = (e.offsetX / coords.width) * 2 - 1;
        coords.height = (e.offsetY / coords.height) * -2 + 1;
        return coords;
    }

    start_rendering(): void {
        this.schedule_render();
        requestAnimationFrame(this.call_render);
    }

    stop_rendering(): void {
        this.render_stop_scheduled = true;
    }

    schedule_render = () => {
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
    }

    protected render(): void {
        this.renderer.render(this.scene, this.camera);
    }

    private on_mouse_down = (e: Event) => {
        if (e.currentTarget) (e.currentTarget as HTMLElement).focus();
    };

    private call_render = () => {
        const controls_updated = this.controls.update(this.controls_clock.getDelta());
        const should_render = this.render_scheduled || controls_updated;

        this.render_scheduled = false;

        if (this.render_stop_scheduled) {
            this.render_stop_scheduled = false;
            return;
        }

        if (should_render) {
            this.render();
        }

        requestAnimationFrame(this.call_render);
    };
}
