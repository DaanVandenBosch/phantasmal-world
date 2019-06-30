import { Object3D, Vector3, Clock } from "three";
import { model_viewer_store } from "../stores/ModelViewerStore";
import { Renderer } from "./Renderer";

let renderer: ModelRenderer | undefined;

export function get_model_renderer(): ModelRenderer {
    if (!renderer) renderer = new ModelRenderer();
    return renderer;
}

export class ModelRenderer extends Renderer {
    private clock = new Clock();

    private model?: Object3D;

    set_model(model?: Object3D) {
        if (this.model !== model) {
            if (this.model) {
                this.scene.remove(this.model);
            }

            if (model) {
                this.scene.add(model);
                this.reset_camera(new Vector3(0, 10, 20), new Vector3(0, 0, 0));
            }

            this.model = model;
        }
    }

    protected render() {
        this.controls.update();

        if (model_viewer_store.animation_mixer) {
            model_viewer_store.animation_mixer.update(this.clock.getDelta());
        }

        this.renderer.render(this.scene, this.camera);
    }
}
