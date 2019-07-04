import Logger from "js-logger";
import { action, observable } from "mobx";
import { AnimationAction, AnimationClip, AnimationMixer, SkinnedMesh } from "three";
import { BufferCursor } from "../data_formats/BufferCursor";
import { NinjaModel, NinjaObject, parse_nj, parse_xj } from "../data_formats/parsing/ninja";
import { parse_njm, NjMotion } from "../data_formats/parsing/ninja/motion";
import { PlayerModel, PlayerAnimation } from "../domain";
import { create_animation_clip, PSO_FRAME_RATE } from "../rendering/animation";
import { ninja_object_to_skinned_mesh } from "../rendering/models";
import { get_player_data, get_player_animation_data } from "./binary_assets";

const logger = Logger.get("stores/ModelViewerStore");
const nj_object_cache: Map<string, Promise<NinjaObject<NinjaModel>>> = new Map();
const nj_motion_cache: Map<number, Promise<NjMotion>> = new Map();

class ModelViewerStore {
    readonly models: PlayerModel[] = [
        new PlayerModel("HUmar", 1, 10, new Set([6])),
        new PlayerModel("HUnewearl", 1, 10, new Set()),
        new PlayerModel("HUcast", 5, 0, new Set()),
        new PlayerModel("HUcaseal", 5, 0, new Set()),
        new PlayerModel("RAmar", 1, 10, new Set([0, 1, 2, 3, 4, 5, 6, 7, 8, 9])),
        new PlayerModel("RAmarl", 1, 10, new Set([0, 1, 2, 3, 4, 5, 6, 7, 8, 9])),
        new PlayerModel("RAcast", 5, 0, new Set()),
        new PlayerModel("RAcaseal", 5, 0, new Set()),
        new PlayerModel("FOmar", 1, 10, new Set([0, 1, 2, 3, 4, 5, 6, 7, 8, 9])),
        new PlayerModel("FOmarl", 1, 10, new Set([0, 1, 2, 3, 4, 5, 6, 7, 8, 9])),
        new PlayerModel("FOnewm", 1, 10, new Set([0, 1, 2, 3, 4, 5, 6, 7, 8, 9])),
        new PlayerModel("FOnewearl", 1, 10, new Set([0, 1, 2, 3, 4, 5, 6, 7, 8, 9])),
    ];
    readonly animations: PlayerAnimation[] = new Array(572)
        .fill(undefined)
        .map((_, i) => new PlayerAnimation(i, `Animation ${i + 1}`));

    @observable.ref current_player_model?: PlayerModel;
    @observable.ref current_model?: NinjaObject<NinjaModel>;
    @observable.ref current_bone_count: number = 0;
    @observable.ref current_obj3d?: SkinnedMesh;

    @observable.ref animation?: {
        player_animation?: PlayerAnimation;
        mixer: AnimationMixer;
        clip: AnimationClip;
        action: AnimationAction;
    };
    @observable animation_playing: boolean = false;
    @observable animation_frame_rate: number = PSO_FRAME_RATE;
    @observable animation_frame: number = 0;
    @observable animation_frame_count: number = 0;

    @observable show_skeleton: boolean = false;

    set_animation_frame_rate = action("set_animation_frame_rate", (rate: number) => {
        if (this.animation) {
            this.animation.mixer.timeScale = rate / PSO_FRAME_RATE;
            this.animation_frame_rate = rate;
        }
    });

    set_animation_frame = action("set_animation_frame", (frame: number) => {
        if (this.animation) {
            const frame_count = this.animation_frame_count;
            if (frame > frame_count) frame = 1;
            if (frame < 1) frame = frame_count;
            this.animation.action.time = (frame - 1) / PSO_FRAME_RATE;
            this.animation_frame = frame;
        }
    });

    load_model = async (model: PlayerModel) => {
        const object = await this.get_player_ninja_object(model);
        this.set_model(object, model);
        // Ignore the bones from the head parts.
        this.current_bone_count = 64;
    };

    load_animation = async (animation: PlayerAnimation) => {
        const nj_motion = await this.get_nj_motion(animation);

        if (this.current_model) {
            this.set_animation(create_animation_clip(this.current_model, nj_motion), animation);
        }
    };

    load_file = (file: File) => {
        const reader = new FileReader();
        reader.addEventListener("loadend", () => {
            this.loadend(file, reader);
        });
        reader.readAsArrayBuffer(file);
    };

    toggle_animation_playing = action("toggle_animation_playing", () => {
        if (this.animation) {
            this.animation.action.paused = !this.animation.action.paused;
            this.animation_playing = !this.animation.action.paused;
        }
    });

    update_animation_frame = action("update_animation_frame", () => {
        if (this.animation && this.animation_playing) {
            const time = this.animation.action.time;
            this.animation_frame = Math.round(time * PSO_FRAME_RATE) + 1;
        }
    });

    set_animation = action("set_animation", (clip: AnimationClip, animation?: PlayerAnimation) => {
        if (!this.current_obj3d) return;

        let mixer: AnimationMixer;

        if (this.animation) {
            this.animation.mixer.stopAllAction();
            mixer = this.animation.mixer;
        } else {
            mixer = new AnimationMixer(this.current_obj3d);
        }

        this.animation = {
            player_animation: animation,
            mixer,
            clip,
            action: mixer.clipAction(clip),
        };

        this.animation.action.play();
        this.animation_playing = true;
        this.animation_frame_count = Math.round(PSO_FRAME_RATE * clip.duration) + 1;
    });

    private set_model = action(
        "set_model",
        (model: NinjaObject<NinjaModel>, player_model?: PlayerModel) => {
            if (this.current_obj3d && this.animation) {
                this.animation.mixer.stopAllAction();
                this.animation.mixer.uncacheRoot(this.current_obj3d);
                this.animation = undefined;
            }

            this.current_player_model = player_model;
            this.current_model = model;
            this.current_bone_count = model.bone_count();

            const mesh = ninja_object_to_skinned_mesh(this.current_model);
            mesh.translateY(-mesh.geometry.boundingSphere.radius);
            this.current_obj3d = mesh;
        }
    );

    // TODO: notify user of problems.
    private loadend = async (file: File, reader: FileReader) => {
        if (!(reader.result instanceof ArrayBuffer)) {
            logger.error("Couldn't read file.");
            return;
        }

        if (file.name.endsWith(".nj")) {
            const model = parse_nj(new BufferCursor(reader.result, true))[0];
            this.set_model(model);
        } else if (file.name.endsWith(".xj")) {
            const model = parse_xj(new BufferCursor(reader.result, true))[0];
            this.set_model(model);
        } else if (file.name.endsWith(".njm")) {
            if (this.current_model) {
                const njm = parse_njm(
                    new BufferCursor(reader.result, true),
                    this.current_bone_count
                );
                this.set_animation(create_animation_clip(this.current_model, njm));
            }
        } else {
            logger.error(`Unknown file extension in filename "${file.name}".`);
        }
    };

    private add_to_bone(
        object: NinjaObject<NinjaModel>,
        head_part: NinjaObject<NinjaModel>,
        bone_id: number
    ): void {
        const bone = object.get_bone(bone_id);

        if (bone) {
            bone.evaluation_flags.hidden = false;
            bone.evaluation_flags.break_child_trace = false;
            bone.children.push(head_part);
        }
    }

    private async get_player_ninja_object(model: PlayerModel): Promise<NinjaObject<NinjaModel>> {
        let ninja_object = nj_object_cache.get(model.name);

        if (ninja_object) {
            return ninja_object;
        } else {
            ninja_object = this.get_all_assets(model);
            nj_object_cache.set(model.name, ninja_object);
            return ninja_object;
        }
    }

    private async get_all_assets(model: PlayerModel): Promise<NinjaObject<NinjaModel>> {
        const body_data = await get_player_data(model.name, "Body");
        const body = parse_nj(new BufferCursor(body_data, true))[0];

        if (!body) {
            throw new Error(`Couldn't parse body for player class ${model.name}.`);
        }

        const head_data = await get_player_data(model.name, "Head", 0);
        const head = parse_nj(new BufferCursor(head_data, true))[0];

        if (head) {
            this.add_to_bone(body, head, 59);
        }

        if (model.hair_styles_count > 0) {
            const hair_data = await get_player_data(model.name, "Hair", 0);
            const hair = parse_nj(new BufferCursor(hair_data, true))[0];

            if (hair) {
                this.add_to_bone(body, hair, 59);
            }

            if (model.hair_styles_with_accessory.has(0)) {
                const accessory_data = await get_player_data(model.name, "Accessory", 0);
                const accessory = parse_nj(new BufferCursor(accessory_data, true))[0];

                if (accessory) {
                    this.add_to_bone(body, accessory, 59);
                }
            }
        }

        return body;
    }

    private async get_nj_motion(animation: PlayerAnimation): Promise<NjMotion> {
        let nj_motion = nj_motion_cache.get(animation.id);

        if (nj_motion) {
            return nj_motion;
        } else {
            nj_motion = get_player_animation_data(animation.id).then(motion_data =>
                parse_njm(new BufferCursor(motion_data, true), this.current_bone_count)
            );

            nj_motion_cache.set(animation.id, nj_motion);
            return nj_motion;
        }
    }
}

export const model_viewer_store = new ModelViewerStore();
