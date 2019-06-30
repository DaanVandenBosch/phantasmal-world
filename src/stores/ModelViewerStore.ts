import Logger from 'js-logger';
import { action, observable } from "mobx";
import { AnimationClip, AnimationMixer, Object3D } from "three";
import { BufferCursor } from "../bin_data/BufferCursor";
import { NinjaModel, NinjaObject, parse_nj, parse_xj } from "../bin_data/parsing/ninja";
import { parse_njm_4 } from "../bin_data/parsing/ninja/motion";
import { create_animation_clip } from "../rendering/animation";
import { ninja_object_to_skinned_mesh } from "../rendering/models";
import { PlayerModel } from '../domain';
import { get_player_ninja_object } from '../bin_data/loading/player';

const logger = Logger.get('stores/ModelViewerStore');

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
    @observable.ref current_model_obj3d?: Object3D;
    @observable.ref animation_mixer?: AnimationMixer;

    load_model = async (model: PlayerModel) => {
        const object = await get_player_ninja_object(model);
        this.set_model(object);
    }

    load_file = (file: File) => {
        const reader = new FileReader();
        reader.addEventListener('loadend', () => { this.loadend(file, reader) });
        reader.readAsArrayBuffer(file);
    }

    private set_model = action('set_model', (model?: NinjaObject<NinjaModel>, filename?: string) => {
        if (this.current_model_obj3d && this.animation_mixer) {
            this.animation_mixer.stopAllAction();
            this.animation_mixer.uncacheRoot(this.current_model_obj3d);
            this.animation_mixer = undefined;
        }

        if (model) {
            if (this.current_model && filename && /^pl[A-Z](hed|hai|cap)\d\d.nj$/.test(filename)) {
                this.add_to_bone(this.current_model, model, 59, [0]);
            } else {
                this.current_model = model;
            }

            const mesh = ninja_object_to_skinned_mesh(this.current_model);
            mesh.translateY(-mesh.geometry.boundingSphere.radius);
            this.current_model_obj3d = mesh;
        } else {
            this.current_model = undefined;
            this.current_model_obj3d = undefined;
        }
    })

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
            this.add_animation(
                create_animation_clip(
                    parse_njm_4(new BufferCursor(reader.result, true))
                )
            );
        } else {
            logger.error(`Unknown file extension in filename "${file.name}".`);
        }
    }

    private add_to_bone(
        object: NinjaObject<NinjaModel>,
        head_part: NinjaObject<NinjaModel>,
        bone_id: number,
        id_ref: [number]
    ) {
        if (!object.evaluation_flags.skip) {
            const id = id_ref[0]++;

            if (id === bone_id) {
                object.evaluation_flags.hidden = false;
                object.evaluation_flags.break_child_trace = false;
                object.children.push(head_part);
                return;
            }
        }

        for (const child of object.children) {
            this.add_to_bone(child, head_part, bone_id, id_ref);
        }
    }

    private add_animation = action('add_animation', (clip: AnimationClip) => {
        if (!this.current_model_obj3d) return;

        if (this.animation_mixer) {
            this.animation_mixer.stopAllAction();
        } else {
            this.animation_mixer = new AnimationMixer(this.current_model_obj3d);
        }

        const action = this.animation_mixer.clipAction(clip);
        action.play();
    })
}

export const model_viewer_store = new ModelViewerStore();
