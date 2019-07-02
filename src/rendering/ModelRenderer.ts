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
            const show_skeleton = model_viewer_store.show_skeleton;

            if (this.skeleton_helper) {
                this.skeleton_helper.visible = show_skeleton;
                this.schedule_render();
            }

            if (!model_viewer_store.animation_playing) {
                // Reference animation_frame here to make sure we render when the user sets the frame manually.
                model_viewer_store.animation_frame;
                this.schedule_render();
            }
        });

        autorun(() => {
            if (model_viewer_store.animation) {
                this.schedule_render();
            }
        });
    }

    set_model(model?: SkinnedMesh): void {
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
            this.schedule_render();
        }
    }

    protected render(): void {
        if (model_viewer_store.animation) {
            model_viewer_store.animation.mixer.update(this.clock.getDelta());
            model_viewer_store.update_animation_frame();
        }

        super.render();

        if (model_viewer_store.animation && !model_viewer_store.animation.action.paused) {
            this.schedule_render();
        }
    }
}
