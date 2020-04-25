import {
    AnimationClip,
    AnimationMixer,
    Clock,
    DoubleSide,
    MeshLambertMaterial,
    Object3D,
    PerspectiveCamera,
    SkeletonHelper,
    SkinnedMesh,
    Vector3,
} from "three";
import { Disposable } from "../../core/observable/Disposable";
import { NjMotion } from "../../core/data_formats/parsing/ninja/motion";
import { xvr_texture_to_three_texture } from "../../core/rendering/conversion/ninja_textures";
import { create_mesh } from "../../core/rendering/conversion/create_mesh";
import { ninja_object_to_buffer_geometry } from "../../core/rendering/conversion/ninja_three_geometry";
import {
    create_animation_clip,
    PSO_FRAME_RATE,
} from "../../core/rendering/conversion/ninja_animation";
import { DisposableThreeRenderer, ThreeRenderer } from "../../core/rendering/ThreeRenderer";
import { Disposer } from "../../core/observable/Disposer";
import { ChangeEvent } from "../../core/observable/Observable";
import { LogManager } from "../../core/Logger";
import { ModelStore } from "../stores/ModelStore";
import { CharacterClassModel } from "../model/CharacterClassModel";

const logger = LogManager.get("viewer/rendering/ModelRenderer");

const DEFAULT_MATERIAL = new MeshLambertMaterial({
    color: 0xffffff,
    side: DoubleSide,
});
const DEFAULT_SKINNED_MATERIAL = new MeshLambertMaterial({
    skinning: true,
    color: 0xffffff,
    side: DoubleSide,
});
const CAMERA_POSITION = Object.freeze(new Vector3(0, 10, 20));
const CAMERA_LOOK_AT = Object.freeze(new Vector3(0, 0, 0));

export class ModelRenderer extends ThreeRenderer implements Disposable {
    private readonly disposer = new Disposer();
    private readonly clock = new Clock();
    private character_class_active: boolean;
    private mesh?: Object3D;
    private skeleton_helper?: SkeletonHelper;
    private animation?: {
        mixer: AnimationMixer;
        clip: AnimationClip;
    };
    private update_animation_time = true;

    readonly camera = new PerspectiveCamera(75, 1, 1, 200);

    constructor(private readonly store: ModelStore, three_renderer: DisposableThreeRenderer) {
        super(three_renderer);

        this.character_class_active = store.current_character_class.val != undefined;

        this.disposer.add_all(
            store.current_character_class.observe(this.current_character_class_changed),
            store.current_nj_object.observe(this.nj_object_or_xvm_changed),
            store.current_textures.observe(this.nj_object_or_xvm_changed),
            store.current_nj_motion.observe(this.nj_motion_changed),
            store.show_skeleton.observe(this.show_skeleton_changed),
            store.animation_playing.observe(this.animation_playing_changed),
            store.animation_frame_rate.observe(this.animation_frame_rate_changed),
            store.animation_frame.observe(this.animation_frame_changed),
        );

        this.init_camera_controls();
        this.reset_camera(CAMERA_POSITION, CAMERA_LOOK_AT);
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

    private current_character_class_changed = (
        change: ChangeEvent<CharacterClassModel | undefined>,
    ): void => {
        const character_class_active = change.value != undefined;

        if (this.character_class_active !== character_class_active) {
            this.reset_camera(CAMERA_POSITION, CAMERA_LOOK_AT);
        }

        this.character_class_active = character_class_active;
    };

    private nj_object_or_xvm_changed = (): void => {
        if (this.mesh) {
            this.scene.remove(this.mesh);
            this.mesh = undefined;
            this.scene.remove(this.skeleton_helper!);
            this.skeleton_helper = undefined;
        }

        const nj_object = this.store.current_nj_object.val;

        if (nj_object) {
            // Stop animation and store animation time.
            let animation_time: number | undefined;

            if (this.animation) {
                const mixer = this.animation.mixer;
                animation_time = mixer.existingAction(this.animation.clip)?.time;
                mixer.stopAllAction();
                mixer.uncacheAction(this.animation.clip);
                this.animation = undefined;
            }

            // Convert textures and geometry.
            const textures = this.store.current_textures.val.map(tex => {
                if (tex) {
                    try {
                        return xvr_texture_to_three_texture(tex);
                    } catch (e) {
                        logger.error("Couldn't convert XVR texture.", e);
                    }
                }

                return undefined;
            });

            const geometry = ninja_object_to_buffer_geometry(nj_object);
            const has_skeleton = geometry.getAttribute("skinIndex") != undefined;

            this.mesh = create_mesh(
                geometry,
                textures,
                has_skeleton ? DEFAULT_SKINNED_MATERIAL : DEFAULT_MATERIAL,
                has_skeleton,
            );

            // Make sure we rotate around the center of the model instead of its origin.
            const bb = geometry.boundingBox!;
            const height = bb.max.y - bb.min.y;
            this.mesh.translateY(-height / 2 - bb.min.y);

            this.scene.add(this.mesh);

            // Add skeleton.
            this.skeleton_helper = new SkeletonHelper(this.mesh);
            this.skeleton_helper.visible = this.store.show_skeleton.val;
            (this.skeleton_helper.material as any).linewidth = 3;
            this.scene.add(this.skeleton_helper);

            // Create a new animation mixer and clip.
            const nj_motion = this.store.current_nj_motion.val;

            if (nj_motion) {
                const mixer = new AnimationMixer(this.mesh);
                mixer.timeScale = this.store.animation_frame_rate.val / PSO_FRAME_RATE;

                const clip = create_animation_clip(nj_object, nj_motion);

                this.animation = { mixer, clip };

                const action = mixer.clipAction(clip, this.mesh);
                action.time = animation_time ?? 0;
                action.play();
            }
        }

        this.schedule_render();
    };

    private nj_motion_changed = ({ value: nj_motion }: ChangeEvent<NjMotion | undefined>): void => {
        let mixer: AnimationMixer | undefined;

        if (this.animation) {
            this.animation.mixer.stopAllAction();
            this.animation.mixer.uncacheAction(this.animation.clip);
            mixer = this.animation.mixer;
        }

        const nj_object = this.store.current_nj_object.val;

        if (!this.mesh || !(this.mesh instanceof SkinnedMesh) || !nj_motion || !nj_object) return;

        if (!mixer) {
            mixer = new AnimationMixer(this.mesh);
        }

        const clip = create_animation_clip(nj_object, nj_motion);

        this.animation = { mixer, clip };

        this.clock.start();
        mixer.clipAction(clip).play();
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
        const nj_motion = this.store.current_nj_motion.val;

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
                this.store.set_animation_frame(action.time * PSO_FRAME_RATE + 1);
                this.update_animation_time = true;
            }
        }
    }
}
