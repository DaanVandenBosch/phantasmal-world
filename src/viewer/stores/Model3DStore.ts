import { ArrayBufferCursor } from "../../core/data_formats/cursor/ArrayBufferCursor";
import { Endianness } from "../../core/data_formats/Endianness";
import { NjMotion, parse_njm } from "../../core/data_formats/parsing/ninja/motion";
import { NjObject, parse_nj, parse_xj } from "../../core/data_formats/parsing/ninja";
import { CharacterClassModel } from "../model/CharacterClassModel";
import { CharacterClassAnimationModel } from "../model/CharacterClassAnimationModel";
import { WritableProperty } from "../../core/observable/property/WritableProperty";
import { read_file } from "../../core/read_file";
import { property } from "../../core/observable";
import { Property } from "../../core/observable/property/Property";
import { PSO_FRAME_RATE } from "../../core/rendering/conversion/ninja_animation";
import { parse_xvm, Xvm } from "../../core/data_formats/parsing/ninja/texture";
import { CharacterClassAssetLoader } from "../loading/CharacterClassAssetLoader";
import { Store } from "../../core/stores/Store";
import { LogManager } from "../../core/Logger";

const logger = LogManager.get("viewer/stores/ModelStore");
const nj_object_cache: Map<string, Promise<NjObject>> = new Map();
const nj_motion_cache: Map<number, Promise<NjMotion>> = new Map();

export type NjData = {
    nj_object: NjObject;
    bone_count: number;
    has_skeleton: boolean;
};

export class Model3DStore extends Store {
    private readonly _current_model: WritableProperty<CharacterClassModel | undefined> = property(
        undefined,
    );
    private readonly _current_nj_data = property<NjData | undefined>(undefined);
    private readonly _current_xvm = property<Xvm | undefined>(undefined);
    private readonly _show_skeleton: WritableProperty<boolean> = property(false);
    private readonly _current_animation: WritableProperty<
        CharacterClassAnimationModel | undefined
    > = property(undefined);
    private readonly _current_nj_motion = property<NjMotion | undefined>(undefined);
    private readonly _animation_playing: WritableProperty<boolean> = property(true);
    private readonly _animation_frame_rate: WritableProperty<number> = property(PSO_FRAME_RATE);
    private readonly _animation_frame: WritableProperty<number> = property(0);

    readonly models: readonly CharacterClassModel[] = [
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

    readonly animations: readonly CharacterClassAnimationModel[] = new Array(572)
        .fill(undefined)
        .map((_, i) => new CharacterClassAnimationModel(i, `Animation ${i + 1}`));

    readonly current_model: Property<CharacterClassModel | undefined> = this._current_model;
    readonly current_nj_data: Property<NjData | undefined> = this._current_nj_data;
    readonly current_xvm: Property<Xvm | undefined> = this._current_xvm;
    readonly show_skeleton: Property<boolean> = this._show_skeleton;
    readonly current_animation: Property<CharacterClassAnimationModel | undefined> = this
        ._current_animation;
    readonly current_nj_motion: Property<NjMotion | undefined> = this._current_nj_motion;
    readonly animation_playing: Property<boolean> = this._animation_playing;
    readonly animation_frame_rate: Property<number> = this._animation_frame_rate;
    readonly animation_frame: Property<number> = this._animation_frame;
    readonly animation_frame_count: Property<number> = this.current_nj_motion.map(njm =>
        njm ? njm.frame_count : 0,
    );

    constructor(private readonly asset_loader: CharacterClassAssetLoader) {
        super();

        this.disposables(
            this.current_model.observe(({ value }) => this.load_model(value)),
            this.current_animation.observe(({ value }) => this.load_animation(value)),
        );
    }

    set_current_model = (current_model: CharacterClassModel): void => {
        this._current_model.val = current_model;
    };

    clear_current_model = (): void => {
        this._current_model.val = undefined;
    };

    set_show_skeleton = (show_skeleton: boolean): void => {
        this._show_skeleton.val = show_skeleton;
    };

    set_current_animation = (animation: CharacterClassAnimationModel): void => {
        this._current_animation.val = animation;
    };

    clear_current_animation = (): void => {
        this._current_animation.val = undefined;
    };

    set_animation_playing = (playing: boolean): void => {
        this._animation_playing.val = playing;
    };

    set_animation_frame_rate = (frame_rate: number): void => {
        this._animation_frame_rate.val = frame_rate;
    };

    set_animation_frame = (frame: number): void => {
        this._animation_frame.val = frame;
    };

    // TODO: notify user of problems.
    load_file = async (file: File): Promise<void> => {
        try {
            const buffer = await read_file(file);
            const cursor = new ArrayBufferCursor(buffer, Endianness.Little);

            if (file.name.endsWith(".nj")) {
                this.clear_current_model();

                const nj_object = parse_nj(cursor)[0];

                this.set_current_nj_data({
                    nj_object,
                    bone_count: nj_object.bone_count(),
                    has_skeleton: true,
                });
            } else if (file.name.endsWith(".xj")) {
                this.clear_current_model();

                const nj_object = parse_xj(cursor)[0];

                this.set_current_nj_data({
                    nj_object,
                    bone_count: 0,
                    has_skeleton: false,
                });
            } else if (file.name.endsWith(".njm")) {
                this.clear_current_animation();
                this._current_nj_motion.val = undefined;

                const nj_data = this.current_nj_data.val;

                if (nj_data) {
                    this.set_animation_playing(true);
                    this._current_nj_motion.val = parse_njm(cursor, nj_data.bone_count);
                }
            } else if (file.name.endsWith(".xvm")) {
                if (this.current_model) {
                    this._current_xvm.val = parse_xvm(cursor);
                }
            } else {
                logger.error(`Unknown file extension in filename "${file.name}".`);
            }
        } catch (e) {
            logger.error("Couldn't read file.", e);
        }
    };

    private load_model = async (model?: CharacterClassModel): Promise<void> => {
        this.clear_current_animation();

        if (model) {
            try {
                const nj_object = await this.get_nj_object(model);

                this.set_current_nj_data({
                    nj_object,
                    // Ignore the bones from the head parts.
                    bone_count: model ? 64 : nj_object.bone_count(),
                    has_skeleton: true,
                });
            } catch (e) {
                logger.error(`Couldn't load model for ${model.name}.`);
                this._current_nj_data.val = undefined;
            }
        } else {
            this._current_nj_data.val = undefined;
        }
    };

    private set_current_nj_data(nj_data: NjData): void {
        this._current_xvm.val = undefined;
        this._current_nj_data.val = nj_data;
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
        const body_data = await this.asset_loader.load_geometry(model.name, "Body");
        const body = parse_nj(new ArrayBufferCursor(body_data, Endianness.Little))[0];

        if (!body) {
            throw new Error(`Couldn't parse body for player class ${model.name}.`);
        }

        const head_data = await this.asset_loader.load_geometry(model.name, "Head", 0);
        const head = parse_nj(new ArrayBufferCursor(head_data, Endianness.Little))[0];

        if (head) {
            this.add_to_bone(body, head, 59);
        }

        if (model.hair_styles_count > 0) {
            const hair_data = await this.asset_loader.load_geometry(model.name, "Hair", 0);
            const hair = parse_nj(new ArrayBufferCursor(hair_data, Endianness.Little))[0];

            if (hair) {
                this.add_to_bone(body, hair, 59);
            }

            if (model.hair_styles_with_accessory.has(0)) {
                const accessory_data = await this.asset_loader.load_geometry(
                    model.name,
                    "Accessory",
                    0,
                );
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

    private load_animation = async (animation?: CharacterClassAnimationModel): Promise<void> => {
        const nj_data = this.current_nj_data.val;

        if (nj_data && animation) {
            try {
                this._current_nj_motion.val = await this.get_nj_motion(
                    animation,
                    nj_data.bone_count,
                );
                this.set_animation_playing(true);
            } catch (e) {
                logger.error(`Couldn't load animation "${animation.name}".`);
                this._current_nj_motion.val = undefined;
            }
        } else {
            this._current_nj_motion.val = undefined;
        }
    };

    private async get_nj_motion(
        animation: CharacterClassAnimationModel,
        bone_count: number,
    ): Promise<NjMotion> {
        let nj_motion = nj_motion_cache.get(animation.id);

        if (nj_motion) {
            return nj_motion;
        } else {
            nj_motion = this.asset_loader
                .load_animation(animation.id)
                .then(motion_data =>
                    parse_njm(new ArrayBufferCursor(motion_data, Endianness.Little), bone_count),
                );

            nj_motion_cache.set(animation.id, nj_motion);
            return nj_motion;
        }
    }
}
