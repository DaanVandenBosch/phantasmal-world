import Logger from 'js-logger';
import { action, observable } from "mobx";
import { AnimationAction, AnimationClip, AnimationMixer, SkinnedMesh } from "three";
import { BufferCursor } from "../data_formats/BufferCursor";
import { NinjaModel, NinjaObject, parse_nj, parse_xj } from "../data_formats/parsing/ninja";
import { parse_njm_4 } from "../data_formats/parsing/ninja/motion";
import { PlayerModel } from '../domain';
import { create_animation_clip, PSO_FRAME_RATE } from "../rendering/animation";
import { ninja_object_to_skinned_mesh } from "../rendering/models";
import { get_player_data } from './binary_assets';

const HEAD_PART_REGEX = /^pl[A-Z](hed|hai|cap)\d\d.nj$/;
const logger = Logger.get('stores/ModelViewerStore');
const cache: Map<string, Promise<NinjaObject<NinjaModel>>> = new Map();

class ModelViewerStore {
    readonly models: PlayerModel[] = [
        new PlayerModel('HUmar', 1, 10, new Set([6])),
        new PlayerModel('HUnewearl', 1, 10, new Set()),
        new PlayerModel('HUcast', 5, 0, new Set()),
        new PlayerModel('HUcaseal', 5, 0, new Set()),
        new PlayerModel('RAmar', 1, 10, new Set([0, 1, 2, 3, 4, 5, 6, 7, 8, 9])),
        new PlayerModel('RAmarl', 1, 10, new Set([0, 1, 2, 3, 4, 5, 6, 7, 8, 9])),
        new PlayerModel('RAcast', 5, 0, new Set()),
        new PlayerModel('RAcaseal', 5, 0, new Set()),
        new PlayerModel('FOmarl', 1, 10, new Set([0, 1, 2, 3, 4, 5, 6, 7, 8, 9])),
        new PlayerModel('FOmar', 1, 10, new Set([0, 1, 2, 3, 4, 5, 6, 7, 8, 9])),
        new PlayerModel('FOnewm', 1, 10, new Set([0, 1, 2, 3, 4, 5, 6, 7, 8, 9])),
        new PlayerModel('FOnewearl', 1, 10, new Set([0, 1, 2, 3, 4, 5, 6, 7, 8, 9])),
    ];

    @observable.ref current_model?: NinjaObject<NinjaModel>;
    @observable.ref current_model_obj3d?: SkinnedMesh;

    @observable.ref animation?: {
        mixer: AnimationMixer,
        clip: AnimationClip,
        action: AnimationAction,
    }
    @observable animation_playing: boolean = false;
    @observable animation_frame_rate: number = PSO_FRAME_RATE;
    @observable animation_frame: number = 0;
    @observable animation_frame_count: number = 0;

    @observable show_skeleton: boolean = false;

    set_animation_frame_rate = action('set_animation_frame_rate', (rate: number) => {
        if (this.animation) {
            this.animation.mixer.timeScale = rate / PSO_FRAME_RATE;
            this.animation_frame_rate = rate;
        }
    })

    set_animation_frame = action('set_animation_frame', (frame: number) => {
        if (this.animation) {
            const frame_count = this.animation_frame_count;
            frame = (frame - 1) % frame_count + 1;
            if (frame < 1) frame = frame_count + frame;
            this.animation.action.time = (frame - 1) / (frame_count - 1);
            this.animation_frame = frame;
        }
    })

    load_model = async (model: PlayerModel) => {
        const object = await this.get_player_ninja_object(model);
        this.set_model(object);
    }

    load_file = (file: File) => {
        const reader = new FileReader();
        reader.addEventListener('loadend', () => { this.loadend(file, reader) });
        reader.readAsArrayBuffer(file);
    }

    toggle_animation_playing = action('toggle_animation_playing', () => {
        if (this.animation) {
            this.animation.action.paused = !this.animation.action.paused;
            this.animation_playing = !this.animation.action.paused;
        }
    })

    update_animation_frame = action('update_animation_frame', () => {
        if (this.animation) {
            const frame_count = this.animation_frame_count;
            this.animation_frame = Math.floor(this.animation.action.time * (frame_count - 1) + 1);
        }
    })

    private set_model = action('set_model',
        (model: NinjaObject<NinjaModel>, filename?: string) => {
            if (this.current_model_obj3d && this.animation) {
                this.animation.mixer.stopAllAction();
                this.animation.mixer.uncacheRoot(this.current_model_obj3d);
            }

            if (this.current_model && filename && HEAD_PART_REGEX.test(filename)) {
                this.add_to_bone(this.current_model, model, 59);
            } else {
                this.current_model = model;
            }

            const mesh = ninja_object_to_skinned_mesh(this.current_model);
            mesh.translateY(-mesh.geometry.boundingSphere.radius);
            this.current_model_obj3d = mesh;

            if (this.animation) {
                this.animation.mixer = new AnimationMixer(mesh);
                this.animation.mixer.timeScale = this.animation_frame_rate / PSO_FRAME_RATE;
                this.animation.action = this.animation.mixer.clipAction(this.animation.clip);
                this.animation.action.paused = !this.animation_playing;
                this.animation.action.play();
            }
        }
    )

    // TODO: notify user of problems.
    private loadend = async (file: File, reader: FileReader) => {
        if (!(reader.result instanceof ArrayBuffer)) {
            logger.error('Couldn\'t read file.');
            return;
        }

        if (file.name.endsWith('.nj')) {
            const model = parse_nj(new BufferCursor(reader.result, true))[0];
            this.set_model(model, file.name);
        } else if (file.name.endsWith('.xj')) {
            const model = parse_xj(new BufferCursor(reader.result, true))[0];
            this.set_model(model, file.name);
        } else if (file.name.endsWith('.njm')) {
            if (this.current_model) {
                const njm = parse_njm_4(new BufferCursor(reader.result, true));
                this.add_animation(create_animation_clip(this.current_model, njm));
            }
        } else {
            logger.error(`Unknown file extension in filename "${file.name}".`);
        }
    }

    private add_to_bone(
        object: NinjaObject<NinjaModel>,
        head_part: NinjaObject<NinjaModel>,
        bone_id: number
    ) {
        const bone = object.find_bone(bone_id);

        if (bone) {
            bone.evaluation_flags.hidden = false;
            bone.evaluation_flags.break_child_trace = false;
            bone.children.push(head_part);
        }
    }

    private add_animation = action('add_animation', (clip: AnimationClip) => {
        if (!this.current_model_obj3d) return;

        let mixer: AnimationMixer;

        if (this.animation) {
            this.animation.mixer.stopAllAction();
            mixer = this.animation.mixer;
        } else {
            mixer = new AnimationMixer(this.current_model_obj3d)
        }

        this.animation = {
            mixer,
            clip,
            action: mixer.clipAction(clip)
        }

        this.animation.action.play();
        this.animation_playing = true;
        this.animation_frame_count = PSO_FRAME_RATE * clip.duration + 1;
    })

    private get_player_ninja_object(model: PlayerModel): Promise<NinjaObject<NinjaModel>> {
        let ninja_object = cache.get(model.name);

        if (ninja_object) {
            return ninja_object;
        } else {
            ninja_object = this.get_all_assets(model);
            cache.set(model.name, ninja_object);
            return ninja_object;
        }
    }

    private async  get_all_assets(model: PlayerModel): Promise<NinjaObject<NinjaModel>> {
        const body_data = await get_player_data(model.name, 'Body');
        const body = parse_nj(new BufferCursor(body_data, true))[0];

        if (!body) {
            throw new Error(`Couldn't parse body for player class ${model.name}.`);
        }

        const head_data = await get_player_data(model.name, 'Head', 0);
        const head = parse_nj(new BufferCursor(head_data, true))[0];

        if (head) {
            this.add_to_bone(body, head, 59);
        }

        if (model.hair_styles_count > 0) {
            const hair_data = await get_player_data(model.name, 'Hair', 0);
            const hair = parse_nj(new BufferCursor(hair_data, true))[0];

            if (hair) {
                this.add_to_bone(body, hair, 59);
            }

            if (model.hair_styles_with_accessory.has(0)) {
                const accessory_data = await get_player_data(model.name, 'Accessory', 0);
                const accessory = parse_nj(new BufferCursor(accessory_data, true))[0];

                if (accessory) {
                    this.add_to_bone(body, accessory, 59);
                }
            }
        }

        return body;
    }
}

export const model_viewer_store = new ModelViewerStore();
