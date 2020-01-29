import { ModelStore } from "../stores/ModelStore";
import { Disposer } from "../../core/observable/Disposer";
import { Renderer } from "../../core/rendering/Renderer";
import { GfxRenderer } from "../../core/rendering/GfxRenderer";
import { ninja_object_to_node } from "../../core/rendering/conversion/ninja_geometry";

export class ModelGfxRenderer implements Renderer {
    private readonly disposer = new Disposer();

    readonly canvas_element: HTMLCanvasElement;

    constructor(private readonly store: ModelStore, private readonly renderer: GfxRenderer) {
        this.canvas_element = renderer.canvas_element;

        renderer.camera.pan(0, 0, -50);

        this.disposer.add_all(store.current_nj_object.observe(this.nj_object_or_xvm_changed));
    }

    dispose(): void {
        this.disposer.dispose();
    }

    start_rendering(): void {
        this.renderer.start_rendering();
    }

    stop_rendering(): void {
        this.renderer.stop_rendering();
    }

    set_size(width: number, height: number): void {
        this.renderer.set_size(width, height);
    }

    private nj_object_or_xvm_changed = (): void => {
        this.renderer.destroy_scene();

        const nj_object = this.store.current_nj_object.val;

        if (nj_object) {
            // Convert textures and geometry.
            const node = ninja_object_to_node(nj_object);
            this.renderer.scene.root_node.add_child(node);

            this.renderer.scene.traverse(node => {
                node.mesh?.upload(this.renderer.gfx);
            }, undefined);
        }

        this.renderer.schedule_render();
    };
}
