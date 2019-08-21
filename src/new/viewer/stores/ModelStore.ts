import { ArrayBufferCursor } from "../../../core/data_formats/cursor/ArrayBufferCursor";
import { Endianness } from "../../../core/data_formats/Endianness";
import { NjMotion, parse_njm } from "../../../core/data_formats/parsing/ninja/motion";
import { NjObject, parse_nj, parse_xj } from "../../../core/data_formats/parsing/ninja";
import { CharacterClassModel } from "../domain/CharacterClassModel";
import { CharacterClassAnimation } from "../domain/CharacterClassAnimation";
import { WritableProperty } from "../../core/observable/WritableProperty";
import {
    get_character_class_animation_data,
    get_character_class_data,
} from "../../../viewer/loading/character_class";
import { Disposable } from "../../core/observable/Disposable";
import { read_file } from "../../../core/read_file";
import { property } from "../../core/observable";
import { Property } from "../../core/observable/Property";
import { PSO_FRAME_RATE } from "../../../core/rendering/conversion/ninja_animation";
import { parse_xvm, Xvm } from "../../../core/data_formats/parsing/ninja/texture";
import Logger = require("js-logger");

const logger = Logger.get("viewer/stores/ModelStore");
const nj_object_cache: Map<string, Promise<NjObject>> = new Map();
const nj_motion_cache: Map<number, Promise<NjMotion>> = new Map();

export type NjData = {
    nj_object: NjObject;
    bone_count: number;
    has_skeleton: boolean;
};

export class ModelStore implements Disposable {
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

    readonly current_model: WritableProperty<CharacterClassModel | undefined> = property(undefined);

    private readonly _current_nj_data = property<NjData | undefined>(undefined);
    readonly current_nj_data: Property<NjData | undefined> = this._current_nj_data;

    private readonly _current_xvm = property<Xvm | undefined>(undefined);
    readonly current_xvm: Property<Xvm | undefined> = this._current_xvm;

    readonly show_skeleton: WritableProperty<boolean> = property(false);

    readonly current_animation: WritableProperty<CharacterClassAnimation | undefined> = property(
        undefined,
    );

    private readonly _current_nj_motion = property<NjMotion | undefined>(undefined);
    readonly current_nj_motion: Property<NjMotion | undefined> = this._current_nj_motion;

    readonly animation_playing: WritableProperty<boolean> = property(true);
    readonly animation_frame_rate: WritableProperty<number> = property(PSO_FRAME_RATE);
    readonly animation_frame: WritableProperty<number> = property(0);
    readonly animation_frame_count: Property<number> = this.current_nj_motion.map(njm =>
        njm ? njm.frame_count : 0,
    );

    private disposables: Disposable[] = [];

    constructor() {
        this.disposables.push(
            this.current_model.observe(this.load_model),
            this.current_animation.observe(this.load_animation),
        );
    }

    dispose(): void {
        this.disposables.forEach(d => d.dispose());
    }

    // TODO: notify user of problems.
    load_file = async (file: File) => {
        try {
            const buffer = await read_file(file);
            const cursor = new ArrayBufferCursor(buffer, Endianness.Little);

            if (file.name.endsWith(".nj")) {
                this.current_model.set(undefined);

                const nj_object = parse_nj(cursor)[0];

                this.set_current_nj_data({
                    nj_object,
                    bone_count: nj_object.bone_count(),
                    has_skeleton: true,
                });
            } else if (file.name.endsWith(".xj")) {
                this.current_model.set(undefined);

                const nj_object = parse_xj(cursor)[0];

                this.set_current_nj_data({
                    nj_object,
                    bone_count: 0,
                    has_skeleton: false,
                });
            } else if (file.name.endsWith(".njm")) {
                this.current_animation.set(undefined);
                this._current_nj_motion.set(undefined);

                const nj_data = this.current_nj_data.get();

                if (nj_data) {
                    this._current_nj_motion.set(parse_njm(cursor, nj_data.bone_count));
                }
            } else if (file.name.endsWith(".xvm")) {
                if (this.current_model) {
                    const xvm = parse_xvm(cursor);
                    this._current_xvm.set(xvm);
                }
            } else {
                logger.error(`Unknown file extension in filename "${file.name}".`);
            }
        } catch (e) {
            logger.error("Couldn't read file.", e);
        }
    };

    private load_model = async (model?: CharacterClassModel) => {
        this.current_animation.set(undefined);

        if (model) {
            const nj_object = await this.get_nj_object(model);

            this.set_current_nj_data({
                nj_object,
                // Ignore the bones from the head parts.
                bone_count: model ? 64 : nj_object.bone_count(),
                has_skeleton: true,
            });
        } else {
            this._current_nj_data.set(undefined);
        }
    };

    private set_current_nj_data(nj_data: NjData): void {
        this.current_model.set(undefined);
        this._current_nj_data.set(undefined);
        this._current_xvm.set(undefined);
        this._current_nj_data.set(nj_data);
    }

    private async get_nj_object(model: CharacterClassModel): Promise<NjObject> {
        let nj_object = nj_object_cache.get(model.name);

        if (nj_object) {
            return nj_object;
        } else {
            nj_object = this.get_all_nj_objects(model);
            nj_object_cache.set(model.name, nj_object);
            return nj_object;
        }
    }

    private async get_all_nj_objects(model: CharacterClassModel): Promise<NjObject> {
        const body_data = await get_character_class_data(model.name, "Body");
        const body = parse_nj(new ArrayBufferCursor(body_data, Endianness.Little))[0];

        if (!body) {
            throw new Error(`Couldn't parse body for player class ${model.name}.`);
        }

        const head_data = await get_character_class_data(model.name, "Head", 0);
        const head = parse_nj(new ArrayBufferCursor(head_data, Endianness.Little))[0];

        if (head) {
            this.add_to_bone(body, head, 59);
        }

        if (model.hair_styles_count > 0) {
            const hair_data = await get_character_class_data(model.name, "Hair", 0);
            const hair = parse_nj(new ArrayBufferCursor(hair_data, Endianness.Little))[0];

            if (hair) {
                this.add_to_bone(body, hair, 59);
            }

            if (model.hair_styles_with_accessory.has(0)) {
                const accessory_data = await get_character_class_data(model.name, "Accessory", 0);
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

    private load_animation = async (animation?: CharacterClassAnimation) => {
        const nj_data = this.current_nj_data.get();

        if (nj_data && animation) {
            this._current_nj_motion.set(await this.get_nj_motion(animation, nj_data.bone_count));
            this.animation_playing.set(true);
        } else {
            this._current_nj_motion.set(undefined);
        }
    };

    private async get_nj_motion(
        animation: CharacterClassAnimation,
        bone_count: number,
    ): Promise<NjMotion> {
        let nj_motion = nj_motion_cache.get(animation.id);

        if (nj_motion) {
            return nj_motion;
        } else {
            nj_motion = get_character_class_animation_data(animation.id).then(motion_data =>
                parse_njm(new ArrayBufferCursor(motion_data, Endianness.Little), bone_count),
            );

            nj_motion_cache.set(animation.id, nj_motion);
            return nj_motion;
        }
    }
}

export const model_store = new ModelStore();
