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
    Vector3,
} from "three";
import { model_store } from "../stores/ModelStore";
import { Disposable } from "../../core/observable/Disposable";
import { NjMotion } from "../../core/data_formats/parsing/ninja/motion";
import { xvm_to_textures } from "../../core/rendering/conversion/ninja_textures";
import { create_mesh, create_skinned_mesh } from "../../core/rendering/conversion/create_mesh";
import { ninja_object_to_buffer_geometry } from "../../core/rendering/conversion/ninja_geometry";
import {
    create_animation_clip,
    PSO_FRAME_RATE,
} from "../../core/rendering/conversion/ninja_animation";
import { Renderer } from "../../core/rendering/Renderer";

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
            model_store.current_nj_data.observe(this.nj_data_or_xvm_changed),
            model_store.current_xvm.observe(this.nj_data_or_xvm_changed),
            model_store.current_nj_motion.observe(this.nj_motion_changed),
            model_store.show_skeleton.observe(this.show_skeleton_changed),
            model_store.animation_playing.observe(this.animation_playing_changed),
            model_store.animation_frame_rate.observe(this.animation_frame_rate_changed),
            model_store.animation_frame.observe(this.animation_frame_changed),
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
        }

        this.light_holder.quaternion.copy(this.perspective_camera.quaternion);
        super.render();

        if (this.animation && !this.animation.action.paused) {
            this.update_animation_frame();
            this.schedule_render();
        }
    }

    private nj_data_or_xvm_changed = () => {
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

        const nj_data = model_store.current_nj_data.get();

        if (nj_data) {
            const { nj_object, has_skeleton } = nj_data;

            let mesh: Mesh;

            const xvm = model_store.current_xvm.get();
            const textures = xvm ? xvm_to_textures(xvm) : undefined;

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
        this.schedule_render();
    };

    private show_skeleton_changed = (show_skeleton: boolean) => {
        if (this.skeleton_helper) {
            this.skeleton_helper.visible = show_skeleton;
            this.schedule_render();
        }
    };

    private animation_playing_changed = (playing: boolean) => {
        if (this.animation) {
            this.animation.action.paused = !playing;

            if (playing) {
                this.clock.start();
                this.schedule_render();
            } else {
                this.clock.stop();
            }
        }
    };

    private animation_frame_rate_changed = (frame_rate: number) => {
        if (this.animation) {
            this.animation.mixer.timeScale = frame_rate / PSO_FRAME_RATE;
        }
    };

    private animation_frame_changed = (frame: number) => {
        const nj_motion = model_store.current_nj_motion.get();

        if (this.animation && nj_motion) {
            const frame_count = nj_motion.frame_count;
            if (frame > frame_count) frame = 1;
            if (frame < 1) frame = frame_count;
            this.animation.action.time = (frame - 1) / PSO_FRAME_RATE;
            this.schedule_render();
        }
    };

    private update_animation_frame(): void {
        if (this.animation && !this.animation.action.paused) {
            const time = this.animation.action.time;
            model_store.animation_frame.set(time * PSO_FRAME_RATE + 1);
        }
    }
}
