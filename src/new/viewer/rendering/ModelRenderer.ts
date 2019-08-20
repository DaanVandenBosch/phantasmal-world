import {
    AnimationAction,
    AnimationClip,
    AnimationMixer,
    Clock,
    DoubleSide,
    Mesh,
    MeshLambertMaterial,
    Object3D,
    PerspectiveCamera,
    SkeletonHelper,
    SkinnedMesh,
    Texture,
    Vector3,
} from "three";
import { Renderer } from "../../../core/rendering/Renderer";
import { model_store } from "../stores/ModelStore";
import { Disposable } from "../../core/gui/Disposable";
import { create_mesh, create_skinned_mesh } from "../../../core/rendering/conversion/create_mesh";
import { ninja_object_to_buffer_geometry } from "../../../core/rendering/conversion/ninja_geometry";
import { NjObject } from "../../../core/data_formats/parsing/ninja";
import {
    create_animation_clip,
    PSO_FRAME_RATE,
} from "../../../core/rendering/conversion/ninja_animation";
import { NjMotion } from "../../../core/data_formats/parsing/ninja/motion";

export class ModelRenderer extends Renderer implements Disposable {
    private readonly perspective_camera: PerspectiveCamera;
    private readonly disposables: Disposable[] = [];
    private readonly clock = new Clock();
    private mesh?: Object3D;
    private skeleton_helper?: SkeletonHelper;
    private animation?: {
        mixer: AnimationMixer;
        clip: AnimationClip;
        action: AnimationAction;
    };

    constructor() {
        super(new PerspectiveCamera(75, 1, 1, 200));

        this.perspective_camera = this.camera as PerspectiveCamera;

        this.disposables.push(
            model_store.current_nj_data.observe(this.nj_object_changed),
            model_store.current_nj_motion.observe(this.nj_motion_changed),
            model_store.show_skeleton.observe(this.show_skeleton_changed),
        );
    }

    set_size(width: number, height: number): void {
        this.perspective_camera.aspect = width / height;
        this.perspective_camera.updateProjectionMatrix();
        super.set_size(width, height);
    }

    dispose(): void {
        super.dispose();
        this.disposables.forEach(d => d.dispose());
    }

    protected render(): void {
        if (this.animation) {
            this.animation.mixer.update(this.clock.getDelta());
            // this.update_animation_frame();
        }

        this.light_holder.quaternion.copy(this.perspective_camera.quaternion);
        super.render();

        if (this.animation && !this.animation.action.paused) {
            this.schedule_render();
        }
    }

    private nj_object_changed = (nj_data?: { nj_object: NjObject; has_skeleton: boolean }) => {
        if (this.mesh) {
            this.scene.remove(this.mesh);
            this.mesh = undefined;
            this.scene.remove(this.skeleton_helper!);
            this.skeleton_helper = undefined;
        }

        if (this.animation) {
            this.animation.mixer.stopAllAction();
            if (this.mesh) this.animation.mixer.uncacheRoot(this.mesh);
            this.animation = undefined;
        }

        if (nj_data) {
            const { nj_object, has_skeleton } = nj_data;

            let mesh: Mesh;

            // TODO:
            const textures: Texture[] | undefined = Math.random() > 1 ? [] : undefined;

            const materials =
                textures &&
                textures.map(
                    tex =>
                        new MeshLambertMaterial({
                            skinning: has_skeleton,
                            map: tex,
                            side: DoubleSide,
                            alphaTest: 0.5,
                        }),
                );

            if (has_skeleton) {
                mesh = create_skinned_mesh(ninja_object_to_buffer_geometry(nj_object), materials);
            } else {
                mesh = create_mesh(ninja_object_to_buffer_geometry(nj_object), materials);
            }

            // Make sure we rotate around the center of the model instead of its origin.
            const bb = mesh.geometry.boundingBox;
            const height = bb.max.y - bb.min.y;
            mesh.translateY(-height / 2 - bb.min.y);

            this.mesh = mesh;
            this.scene.add(mesh);

            this.skeleton_helper = new SkeletonHelper(mesh);
            this.skeleton_helper.visible = model_store.show_skeleton.get();
            (this.skeleton_helper.material as any).linewidth = 3;
            this.scene.add(this.skeleton_helper);

            this.reset_camera(new Vector3(0, 10, 20), new Vector3(0, 0, 0));
        }

        this.schedule_render();
    };

    private nj_motion_changed = (nj_motion?: NjMotion) => {
        let mixer!: AnimationMixer;

        if (this.animation) {
            this.animation.mixer.stopAllAction();
            mixer = this.animation.mixer;
        }

        const nj_data = model_store.current_nj_data.get();

        if (!this.mesh || !(this.mesh instanceof SkinnedMesh) || !nj_motion || !nj_data) return;

        if (!this.animation) {
            mixer = new AnimationMixer(this.mesh);
        }

        const clip = create_animation_clip(nj_data.nj_object, nj_motion);

        this.animation = {
            mixer,
            clip,
            action: mixer.clipAction(clip),
        };

        this.clock.start();
        this.animation.action.play();
        // TODO:
        // this.animation_playing = true;
        // this.animation_frame_count = Math.round(PSO_FRAME_RATE * clip.duration) + 1;
        this.schedule_render();
    };

    private show_skeleton_changed = (show_skeleton: boolean) => {
        if (this.skeleton_helper) {
            this.skeleton_helper.visible = show_skeleton;
            this.schedule_render();
        }
    };

    private update(): void {
        // if (!model_viewer_store.animation_playing) {
        //     // Reference animation_frame here to make sure we render when the user sets the frame manually.
        //     model_viewer_store.animation_frame;
        //     this.schedule_render();
        // }

        this.schedule_render();
    }
}
