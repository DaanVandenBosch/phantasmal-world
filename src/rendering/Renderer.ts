import * as THREE from 'three';
import { Color, HemisphereLight, MOUSE, PerspectiveCamera, Scene, Vector3, WebGLRenderer } from 'three';
import OrbitControlsCreator from 'three-orbit-controls';

const OrbitControls = OrbitControlsCreator(THREE);

export class Renderer {
    protected renderer = new WebGLRenderer({ antialias: true });
    protected camera: PerspectiveCamera;
    protected controls: any;
    protected scene = new Scene();

    constructor() {
        this.camera = new PerspectiveCamera(75, 1, 0.1, 5000);
        this.controls = new OrbitControls(
            this.camera,
            this.renderer.domElement
        );
        this.controls.mouseButtons.ORBIT = MOUSE.RIGHT;
        this.controls.mouseButtons.PAN = MOUSE.LEFT;

        this.scene.background = new Color(0x151C21);
        this.scene.add(new HemisphereLight(0xffffff, 0x505050, 1));

        requestAnimationFrame(this.render_loop);
    }

    get dom_element(): HTMLElement {
        return this.renderer.domElement;
    }

    set_size(width: number, height: number) {
        this.renderer.setSize(width, height);
        this.camera.aspect = width / height;
        this.camera.updateProjectionMatrix();
    }

    protected reset_camera(position: Vector3, look_at: Vector3) {
        this.controls.reset();
        this.camera.position.copy(position);
        this.camera.lookAt(look_at);
    }

    protected render() {
        this.controls.update();
        this.renderer.render(this.scene, this.camera);
    }

    private render_loop = () => {
        this.render();
        requestAnimationFrame(this.render_loop);
    }
}
