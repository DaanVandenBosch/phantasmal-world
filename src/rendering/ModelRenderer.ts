import { autorun } from "mobx";
import { Clock, SkeletonHelper, SkinnedMesh, Vector3 } from "three";
import { model_viewer_store } from "../stores/ModelViewerStore";
import { Renderer } from "./Renderer";

let renderer: ModelRenderer | undefined;

export function get_model_renderer(): ModelRenderer {
    if (!renderer) renderer = new ModelRenderer();
    return renderer;
}

export class ModelRenderer extends Renderer {
    private clock = new Clock();

    private model?: SkinnedMesh;
    private skeleton_helper?: SkeletonHelper;

    constructor() {
        super();
        autorun(() => {
            const show = model_viewer_store.show_skeleton;

            if (this.skeleton_helper) {
                this.skeleton_helper.visible = show;
            }
        });
    }

    set_model(model?: SkinnedMesh) {
        if (this.model !== model) {
            if (this.model) {
                this.scene.remove(this.model);
                this.scene.remove(this.skeleton_helper!);
                this.skeleton_helper = undefined;
            }

            if (model) {
                this.scene.add(model);
                this.skeleton_helper = new SkeletonHelper(model);
                this.skeleton_helper.visible = model_viewer_store.show_skeleton;
                (this.skeleton_helper.material as any).linewidth = 3;
                this.scene.add(this.skeleton_helper);
                this.reset_camera(new Vector3(0, 10, 20), new Vector3(0, 0, 0));
            }

            this.model = model;
        }
    }

    protected render() {
        this.controls.update();

        if (model_viewer_store.animation) {
            model_viewer_store.animation.mixer.update(this.clock.getDelta());
            model_viewer_store.update_animation_frame();
        }

        this.renderer.render(this.scene, this.camera);
    }
}
