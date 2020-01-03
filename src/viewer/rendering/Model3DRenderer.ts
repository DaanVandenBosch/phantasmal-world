import {
    AdditiveBlending,
    AnimationClip,
    AnimationMixer,
    Clock,
    DoubleSide,
    Mesh,
    MeshBasicMaterial,
    MeshLambertMaterial,
    Object3D,
    PerspectiveCamera,
    SkeletonHelper,
    SkinnedMesh,
    Vector3,
} from "three";
import { Disposable } from "../../core/observable/Disposable";
import { NjMotion } from "../../core/data_formats/parsing/ninja/motion";
import { xvr_texture_to_texture } from "../../core/rendering/conversion/ninja_textures";
import { create_mesh, create_skinned_mesh } from "../../core/rendering/conversion/create_mesh";
import { ninja_object_to_buffer_geometry } from "../../core/rendering/conversion/ninja_geometry";
import {
    create_animation_clip,
    PSO_FRAME_RATE,
} from "../../core/rendering/conversion/ninja_animation";
import { DisposableThreeRenderer, Renderer } from "../../core/rendering/Renderer";
import { Disposer } from "../../core/observable/Disposer";
import { ChangeEvent } from "../../core/observable/Observable";
import { Model3DStore } from "../stores/Model3DStore";
import { LogManager } from "../../core/Logger";

const logger = LogManager.get("viewer/rendering/Model3DRenderer");

const DEFAULT_MATERIAL = new MeshLambertMaterial({
    color: 0xffffff,
    side: DoubleSide,
});
const DEFAULT_SKINNED_MATERIAL = new MeshLambertMaterial({
    skinning: true,
    color: 0xffffff,
    side: DoubleSide,
});

export class Model3DRenderer extends Renderer implements Disposable {
    private readonly disposer = new Disposer();
    private readonly clock = new Clock();
    private mesh?: Object3D;
    private skeleton_helper?: SkeletonHelper;
    private animation?: {
        mixer: AnimationMixer;
        clip: AnimationClip;
    };
    private update_animation_time = true;

    readonly camera = new PerspectiveCamera(75, 1, 1, 200);

    constructor(
        three_renderer: DisposableThreeRenderer,
        private readonly model_3d_store: Model3DStore,
    ) {
        super(three_renderer);

        this.disposer.add_all(
            model_3d_store.current_nj_data.observe(this.nj_data_or_xvm_changed),
            model_3d_store.current_textures.observe(this.nj_data_or_xvm_changed),
            model_3d_store.current_nj_motion.observe(this.nj_motion_changed),
            model_3d_store.show_skeleton.observe(this.show_skeleton_changed),
            model_3d_store.animation_playing.observe(this.animation_playing_changed),
            model_3d_store.animation_frame_rate.observe(this.animation_frame_rate_changed),
            model_3d_store.animation_frame.observe(this.animation_frame_changed),
        );

        this.init_camera_controls();
    }

    set_size(width: number, height: number): void {
        this.camera.aspect = width / height;
        this.camera.updateProjectionMatrix();
        super.set_size(width, height);
    }

    dispose(): void {
        super.dispose();
        this.disposer.dispose();
    }

    protected render(): void {
        if (this.animation) {
            this.animation.mixer.update(this.clock.getDelta());
        }

        this.light_holder.quaternion.copy(this.camera.quaternion);
        super.render();

        if (this.animation && !this.animation.mixer.clipAction(this.animation.clip).paused) {
            this.update_animation_frame();
            this.schedule_render();
        }
    }

    private nj_data_or_xvm_changed = (): void => {
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

        const nj_data = this.model_3d_store.current_nj_data.val;

        if (nj_data) {
            const { nj_object, has_skeleton } = nj_data;

            let mesh: Mesh;

            const textures = this.model_3d_store.current_textures.val.map(tex => {
                if (tex) {
                    try {
                        return xvr_texture_to_texture(tex);
                    } catch (e) {
                        logger.error("Couldn't convert XVR texture.", e);
                    }
                }

                return undefined;
            });

            const materials = textures.map(tex =>
                tex
                    ? new MeshBasicMaterial({
                          skinning: has_skeleton,
                          map: tex,
                          side: DoubleSide,
                          alphaTest: 0.1,
                          transparent: true,
                      })
                    : new MeshLambertMaterial({
                          skinning: has_skeleton,
                          side: DoubleSide,
                      }),
            );

            if (has_skeleton) {
                mesh = create_skinned_mesh(
                    ninja_object_to_buffer_geometry(nj_object),
                    materials,
                    DEFAULT_SKINNED_MATERIAL,
                );
            } else {
                mesh = create_mesh(
                    ninja_object_to_buffer_geometry(nj_object),
                    materials,
                    DEFAULT_MATERIAL,
                );
            }

            // Make sure we rotate around the center of the model instead of its origin.
            const bb = mesh.geometry.boundingBox;
            const height = bb.max.y - bb.min.y;
            mesh.translateY(-height / 2 - bb.min.y);

            this.mesh = mesh;
            this.scene.add(mesh);

            this.skeleton_helper = new SkeletonHelper(mesh);
            this.skeleton_helper.visible = this.model_3d_store.show_skeleton.val;
            (this.skeleton_helper.material as any).linewidth = 3;
            this.scene.add(this.skeleton_helper);

            this.reset_camera(new Vector3(0, 10, 20), new Vector3(0, 0, 0));
        }

        this.schedule_render();
    };

    private nj_motion_changed = ({ value: nj_motion }: ChangeEvent<NjMotion | undefined>): void => {
        let mixer!: AnimationMixer;

        if (this.animation) {
            this.animation.mixer.stopAllAction();
            mixer = this.animation.mixer;
        }

        const nj_data = this.model_3d_store.current_nj_data.val;

        if (!this.mesh || !(this.mesh instanceof SkinnedMesh) || !nj_motion || !nj_data) return;

        if (!this.animation) {
            mixer = new AnimationMixer(this.mesh);
        }

        const clip = create_animation_clip(nj_data.nj_object, nj_motion);

        this.animation = {
            mixer,
            clip,
        };

        this.clock.start();
        this.animation.mixer.clipAction(this.animation.clip).play();
        this.schedule_render();
    };

    private show_skeleton_changed = ({ value: show_skeleton }: ChangeEvent<boolean>): void => {
        if (this.skeleton_helper) {
            this.skeleton_helper.visible = show_skeleton;
            this.schedule_render();
        }
    };

    private animation_playing_changed = ({ value: playing }: ChangeEvent<boolean>): void => {
        if (this.animation) {
            this.animation.mixer.clipAction(this.animation.clip).paused = !playing;

            if (playing) {
                this.clock.start();
                this.schedule_render();
            } else {
                this.clock.stop();
            }
        }
    };

    private animation_frame_rate_changed = ({ value: frame_rate }: ChangeEvent<number>): void => {
        if (this.animation) {
            this.animation.mixer.timeScale = frame_rate / PSO_FRAME_RATE;
        }
    };

    private animation_frame_changed = ({ value: frame }: ChangeEvent<number>): void => {
        const nj_motion = this.model_3d_store.current_nj_motion.val;

        if (this.animation && nj_motion) {
            const frame_count = nj_motion.frame_count;
            if (frame > frame_count) frame = 1;
            if (frame < 1) frame = frame_count;

            if (this.update_animation_time) {
                this.animation.mixer.clipAction(this.animation.clip).time =
                    (frame - 1) / PSO_FRAME_RATE;
            }

            this.schedule_render();
        }
    };

    private update_animation_frame(): void {
        if (this.animation) {
            const action = this.animation.mixer.clipAction(this.animation.clip);

            if (!action.paused) {
                this.update_animation_time = false;
                this.model_3d_store.set_animation_frame(action.time * PSO_FRAME_RATE + 1);
                this.update_animation_time = true;
            }
        }
    }
}
