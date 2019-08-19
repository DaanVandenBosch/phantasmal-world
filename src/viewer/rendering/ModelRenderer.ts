import { autorun } from "mobx";
import { Object3D, PerspectiveCamera, SkeletonHelper, Vector3 } from "three";
import { model_viewer_store } from "../stores/ModelViewerStore";
import { Renderer } from "../../core/rendering/Renderer";

let renderer: ModelRenderer | undefined;

export function get_model_renderer(): ModelRenderer {
    if (!renderer) renderer = new ModelRenderer();
    return renderer;
}

export class ModelRenderer extends Renderer {
    private model?: Object3D;
    private skeleton_helper?: SkeletonHelper;
    private perspective_camera: PerspectiveCamera;

    constructor() {
        super(new PerspectiveCamera(75, 1, 1, 200));

        this.perspective_camera = this.camera as PerspectiveCamera;

        autorun(() => {
            this.set_model(model_viewer_store.current_obj3d);

            const show_skeleton = model_viewer_store.show_skeleton;

            if (this.skeleton_helper) {
                this.skeleton_helper.visible = show_skeleton;
                this.schedule_render();
            }

            if (model_viewer_store.animation) {
                this.schedule_render();
            }

            if (!model_viewer_store.animation_playing) {
                // Reference animation_frame here to make sure we render when the user sets the frame manually.
                model_viewer_store.animation_frame;
                this.schedule_render();
            }
        });
    }

    set_size(width: number, height: number): void {
        this.perspective_camera.aspect = width / height;
        this.perspective_camera.updateProjectionMatrix();
        super.set_size(width, height);
    }

    protected render(): void {
        if (model_viewer_store.animation) {
            model_viewer_store.animation.mixer.update(model_viewer_store.clock.getDelta());
            model_viewer_store.update_animation_frame();
        }

        this.light_holder.quaternion.copy(this.perspective_camera.quaternion);
        super.render();

        if (model_viewer_store.animation && !model_viewer_store.animation.action.paused) {
            this.schedule_render();
        }
    }

    private set_model(model?: Object3D): void {
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
}
