import { Clock } from "three";
import { ArrayBufferCursor } from "../../../core/data_formats/cursor/ArrayBufferCursor";
import { Endianness } from "../../../core/data_formats/Endianness";
import { NjMotion, parse_njm } from "../../../core/data_formats/parsing/ninja/motion";
import { NjObject, parse_nj, parse_xj } from "../../../core/data_formats/parsing/ninja";
import { CharacterClassModel } from "../domain/CharacterClassModel";
import { CharacterClassAnimation } from "../domain/CharacterClassAnimation";
import { Observable } from "../../core/observable/Observable";
import { get_player_data } from "../../../viewer/loading/player";
import { Disposable } from "../../core/gui/Disposable";
import { read_file } from "../../../core/read_file";
import { create_animation_clip } from "../../../core/rendering/conversion/ninja_animation";
import { parse_xvm } from "../../../core/data_formats/parsing/ninja/texture";
import { xvm_to_textures } from "../../../core/rendering/conversion/ninja_textures";
import Logger = require("js-logger");

const logger = Logger.get("viewer/stores/ModelStore");
const nj_object_cache: Map<string, Promise<NjObject>> = new Map();
const nj_motion_cache: Map<number, Promise<NjMotion>> = new Map();

// TODO: move all Three.js stuff into the renderer.
class ModelStore implements Disposable {
    readonly models: CharacterClassModel[] = [
        new CharacterClassModel("HUmar", 1, 10, new Set([6])),
        new CharacterClassModel("HUnewearl", 1, 10, new Set()),
        new CharacterClassModel("HUcast", 5, 0, new Set()),
        new CharacterClassModel("HUcaseal", 5, 0, new Set()),
        new CharacterClassModel("RAmar", 1, 10, new Set([0, 1, 2, 3, 4, 5, 6, 7, 8, 9])),
        new CharacterClassModel("RAmarl", 1, 10, new Set([0, 1, 2, 3, 4, 5, 6, 7, 8, 9])),
        new CharacterClassModel("RAcast", 5, 0, new Set()),
        new CharacterClassModel("RAcaseal", 5, 0, new Set()),
        new CharacterClassModel("FOmar", 1, 10, new Set([0, 1, 2, 3, 4, 5, 6, 7, 8, 9])),
        new CharacterClassModel("FOmarl", 1, 10, new Set([0, 1, 2, 3, 4, 5, 6, 7, 8, 9])),
        new CharacterClassModel("FOnewm", 1, 10, new Set([0, 1, 2, 3, 4, 5, 6, 7, 8, 9])),
        new CharacterClassModel("FOnewearl", 1, 10, new Set([0, 1, 2, 3, 4, 5, 6, 7, 8, 9])),
    ];

    readonly animations: CharacterClassAnimation[] = new Array(572)
        .fill(undefined)
        .map((_, i) => new CharacterClassAnimation(i, `Animation ${i + 1}`));

    readonly clock = new Clock();

    readonly current_model = new Observable<CharacterClassModel | undefined>(undefined);

    readonly current_nj_data = new Observable<
        | {
              nj_object: NjObject;
              bone_count: number;
              has_skeleton: boolean;
          }
        | undefined
    >(undefined);

    readonly current_animation = new Observable<CharacterClassAnimation | undefined>(undefined);

    // @observable.ref animation?: {
    //     player_animation?: CharacterClassAnimation;
    //     mixer: AnimationMixer;
    //     clip: AnimationClip;
    //     action: AnimationAction;
    // };
    // @observable animation_playing: boolean = false;
    // @observable animation_frame_rate: number = PSO_FRAME_RATE;
    // @observable animation_frame: number = 0;
    // @observable animation_frame_count: number = 0;

    readonly show_skeleton = new Observable(false);

    private disposables: Disposable[] = [];

    constructor() {
        this.disposables.push(this.current_model.observe(this.load_model));
    }

    dispose(): void {
        this.disposables.forEach(d => d.dispose());
    }

    // set_animation_frame_rate = (rate: number) => {
    //     if (this.animation) {
    //         this.animation.mixer.timeScale = rate / PSO_FRAME_RATE;
    //         this.animation_frame_rate = rate;
    //     }
    // };
    //
    // set_animation_frame = (frame: number) => {
    //     if (this.animation) {
    //         const frame_count = this.animation_frame_count;
    //         if (frame > frame_count) frame = 1;
    //         if (frame < 1) frame = frame_count;
    //         this.animation.action.time = (frame - 1) / PSO_FRAME_RATE;
    //         this.animation_frame = frame;
    //     }
    // };

    // load_animation = async (animation: CharacterClassAnimation) => {
    //     const nj_motion = await this.get_nj_motion(animation);
    //     const nj_data = this.current_nj_data.get();
    //
    //     if (nj_data) {
    //         this.set_animation(create_animation_clip(nj_data, nj_motion), animation);
    //     }
    // };

    // TODO: notify user of problems.
    load_file = async (file: File) => {
        try {
            this.current_model.set(undefined);
            this.current_nj_data.set(undefined);
            this.current_animation.set(undefined);

            const buffer = await read_file(file);
            const cursor = new ArrayBufferCursor(buffer, Endianness.Little);

            if (file.name.endsWith(".nj")) {
                const nj_object = parse_nj(cursor)[0];

                this.current_nj_data.set({
                    nj_object,
                    bone_count: nj_object.bone_count(),
                    has_skeleton: true,
                });
            } else if (file.name.endsWith(".xj")) {
                const nj_object = parse_xj(cursor)[0];

                this.current_nj_data.set({
                    nj_object,
                    bone_count: 0,
                    has_skeleton: false,
                });
                // } else if (file.name.endsWith(".njm")) {
                //     if (this.current_model) {
                //         const njm = parse_njm(cursor, this.current_bone_count);
                //         this.set_animation(create_animation_clip(this.current_model, njm));
                //     }
                // } else if (file.name.endsWith(".xvm")) {
                //     if (this.current_model) {
                //         const xvm = parse_xvm(cursor);
                //         this.set_textures(xvm_to_textures(xvm));
                //     }
            } else {
                logger.error(`Unknown file extension in filename "${file.name}".`);
            }
        } catch (e) {
            logger.error("Couldn't read file.", e);
        }
    };

    // pause_animation = () => {
    //     if (this.animation) {
    //         this.animation.action.paused = true;
    //         this.animation_playing = false;
    //         this.clock.stop();
    //     }
    // };
    //
    // toggle_animation_playing = () => {
    //     if (this.animation) {
    //         this.animation.action.paused = !this.animation.action.paused;
    //         this.animation_playing = !this.animation.action.paused;
    //
    //         if (this.animation_playing) {
    //             this.clock.start();
    //         } else {
    //             this.clock.stop();
    //         }
    //     }
    // };

    // update_animation_frame = () => {
    //     if (this.animation && this.animation_playing) {
    //         const time = this.animation.action.time;
    //         this.animation_frame = Math.round(time * PSO_FRAME_RATE) + 1;
    //     }
    // };

    // set_animation = (clip: AnimationClip, animation?: CharacterClassAnimation) => {
    //     if (!this.current_obj3d || !(this.current_obj3d instanceof SkinnedMesh)) return;
    //
    //     let mixer: AnimationMixer;
    //
    //     if (this.animation) {
    //         this.animation.mixer.stopAllAction();
    //         mixer = this.animation.mixer;
    //     } else {
    //         mixer = new AnimationMixer(this.current_obj3d);
    //     }
    //
    //     this.animation = {
    //         player_animation: animation,
    //         mixer,
    //         clip,
    //         action: mixer.clipAction(clip),
    //     };
    //
    //     this.clock.start();
    //     this.animation.action.play();
    //     this.animation_playing = true;
    //     this.animation_frame_count = Math.round(PSO_FRAME_RATE * clip.duration) + 1;
    // };

    private load_model = async (model?: CharacterClassModel) => {
        if (model) {
            const nj_object = await this.get_player_nj_object(model);
            // if (this.current_obj3d && this.animation) {
            //     this.animation.mixer.stopAllAction();
            //     this.animation.mixer.uncacheRoot(this.current_obj3d);
            //     this.animation = undefined;
            // }

            this.current_nj_data.set({
                nj_object,
                // Ignore the bones from the head parts.
                bone_count: model ? 64 : nj_object.bone_count(),
                has_skeleton: true,
            });
        } else {
            this.current_nj_data.set(undefined);
        }
    };

    private async get_player_nj_object(model: CharacterClassModel): Promise<NjObject> {
        let nj_object = nj_object_cache.get(model.name);

        if (nj_object) {
            return nj_object;
        } else {
            nj_object = this.get_all_assets(model);
            nj_object_cache.set(model.name, nj_object);
            return nj_object;
        }
    }

    private async get_all_assets(model: CharacterClassModel): Promise<NjObject> {
        const body_data = await get_player_data(model.name, "Body");
        const body = parse_nj(new ArrayBufferCursor(body_data, Endianness.Little))[0];

        if (!body) {
            throw new Error(`Couldn't parse body for player class ${model.name}.`);
        }

        const head_data = await get_player_data(model.name, "Head", 0);
        const head = parse_nj(new ArrayBufferCursor(head_data, Endianness.Little))[0];

        if (head) {
            this.add_to_bone(body, head, 59);
        }

        if (model.hair_styles_count > 0) {
            const hair_data = await get_player_data(model.name, "Hair", 0);
            const hair = parse_nj(new ArrayBufferCursor(hair_data, Endianness.Little))[0];

            if (hair) {
                this.add_to_bone(body, hair, 59);
            }

            if (model.hair_styles_with_accessory.has(0)) {
                const accessory_data = await get_player_data(model.name, "Accessory", 0);
                const accessory = parse_nj(
                    new ArrayBufferCursor(accessory_data, Endianness.Little),
                )[0];

                if (accessory) {
                    this.add_to_bone(body, accessory, 59);
                }
            }
        }

        return body;
    }

    private add_to_bone(object: NjObject, head_part: NjObject, bone_id: number): void {
        const bone = object.get_bone(bone_id);

        if (bone) {
            bone.evaluation_flags.hidden = false;
            bone.evaluation_flags.break_child_trace = false;
            bone.children.push(head_part);
        }
    }

    // private async get_nj_motion(animation: CharacterClassAnimation): Promise<NjMotion> {
    //     let nj_motion = nj_motion_cache.get(animation.id);
    //
    //     if (nj_motion) {
    //         return nj_motion;
    //     } else {
    //         nj_motion = get_player_animation_data(animation.id).then(motion_data =>
    //             parse_njm(
    //                 new ArrayBufferCursor(motion_data, Endianness.Little),
    //                 this.current_bone_count,
    //             ),
    //         );
    //
    //         nj_motion_cache.set(animation.id, nj_motion);
    //         return nj_motion;
    //     }
    // }
    //
    // private set_textures = (textures: Texture[]) => {
    //     this.set_obj3d(textures);
    // };
}

export const model_store = new ModelStore();
