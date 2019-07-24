import * as THREE from "three";
import {
    Camera,
    Color,
    Group,
    HemisphereLight,
    MOUSE,
    Scene,
    Vector2,
    Vector3,
    WebGLRenderer,
} from "three";
import OrbitControlsCreator from "three-orbit-controls";

const OrbitControls = OrbitControlsCreator(THREE);

export class Renderer<C extends Camera> {
    protected _debug = false;

    get debug(): boolean {
        return this._debug;
    }

    set debug(debug: boolean) {
        this._debug = debug;
    }

    readonly camera: C;
    readonly controls: any;
    readonly scene = new Scene();
    readonly light_holder = new Group();

    private renderer = new WebGLRenderer({ antialias: true });
    private render_scheduled = false;
    private light = new HemisphereLight(0xffffff, 0x505050, 1.2);

    constructor(camera: C) {
        this.camera = camera;

        this.dom_element.tabIndex = 0;
        this.dom_element.addEventListener("mousedown", this.on_mouse_down);
        this.dom_element.style.outline = "none";

        this.controls = new OrbitControls(camera, this.dom_element);
        this.controls.mouseButtons.ORBIT = MOUSE.RIGHT;
        this.controls.mouseButtons.PAN = MOUSE.LEFT;
        this.controls.addEventListener("change", this.schedule_render);

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

    schedule_render = () => {
        if (!this.render_scheduled) {
            this.render_scheduled = true;
            requestAnimationFrame(this.call_render);
        }
    };

    reset_camera(position: Vector3, look_at: Vector3): void {
        this.controls.reset();
        this.camera.position.copy(position);
        this.camera.lookAt(look_at);
    }

    protected render(): void {
        this.renderer.render(this.scene, this.camera);
    }

    private on_mouse_down = (e: Event) => {
        if (e.currentTarget) (e.currentTarget as HTMLElement).focus();
    };

    private call_render = () => {
        this.render_scheduled = false;
        this.render();
    };
}
