import * as THREE from "three";
import {
    Color,
    HemisphereLight,
    MOUSE,
    PerspectiveCamera,
    Scene,
    Vector3,
    WebGLRenderer,
    Vector2,
    Group,
} from "three";
import OrbitControlsCreator from "three-orbit-controls";

const OrbitControls = OrbitControlsCreator(THREE);

export class Renderer {
    protected camera: PerspectiveCamera;
    protected controls: any;
    protected scene = new Scene();
    protected light_holder = new Group();

    private renderer = new WebGLRenderer({ antialias: true });
    private render_scheduled = false;
    private light = new HemisphereLight(0xffffff, 0x505050, 1);

    constructor() {
        this.renderer.setPixelRatio(window.devicePixelRatio);

        this.camera = new PerspectiveCamera(75, 1, 0.1, 5000);

        this.controls = new OrbitControls(this.camera, this.renderer.domElement);
        this.controls.mouseButtons.ORBIT = MOUSE.RIGHT;
        this.controls.mouseButtons.PAN = MOUSE.LEFT;
        this.controls.addEventListener("change", this.schedule_render);

        this.scene.background = new Color(0x151c21);

        this.light_holder.add(this.light);
        this.scene.add(this.light_holder);
    }

    get dom_element(): HTMLElement {
        return this.renderer.domElement;
    }

    set_size(width: number, height: number): void {
        this.renderer.setSize(width, height);
        this.camera.aspect = width / height;
        this.camera.updateProjectionMatrix();
        this.schedule_render();
    }

    protected schedule_render = () => {
        if (!this.render_scheduled) {
            this.render_scheduled = true;
            requestAnimationFrame(this.call_render);
        }
    };

    protected render(): void {
        this.renderer.render(this.scene, this.camera);
    }

    protected reset_camera(position: Vector3, look_at: Vector3): void {
        this.controls.reset();
        this.camera.position.copy(position);
        this.camera.lookAt(look_at);
    }

    protected pointer_pos_to_device_coords(e: MouseEvent): Vector2 {
        const coords = new Vector2();
        this.renderer.getSize(coords);
        coords.width = (e.offsetX / coords.width) * 2 - 1;
        coords.height = (e.offsetY / coords.height) * -2 + 1;
        return coords;
    }

    private call_render = () => {
        this.render_scheduled = false;
        this.render();
    };
}
