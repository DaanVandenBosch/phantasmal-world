import { Store } from "../../core/stores/Store";
import { WritableProperty } from "../../core/observable/property/WritableProperty";
import {
    CharacterClassModel,
    FOMAR,
    FOMARL,
    FONEWEARL,
    FONEWM,
    HUCASEAL,
    HUCAST,
    HUMAR,
    HUNEWEARL,
    RACASEAL,
    RACAST,
    RAMAR,
    RAMARL,
} from "../model/CharacterClassModel";
import { list_property, property } from "../../core/observable";
import { XvrTexture } from "../../core/data_formats/parsing/ninja/texture";
import { CharacterClassAnimationModel } from "../model/CharacterClassAnimationModel";
import { NjMotion } from "../../core/data_formats/parsing/ninja/motion";
import { PSO_FRAME_RATE } from "../../core/rendering/conversion/ninja_animation";
import { Property } from "../../core/observable/property/Property";
import { ListProperty } from "../../core/observable/property/list/ListProperty";
import { CharacterClassAssetLoader } from "../loading/CharacterClassAssetLoader";
import { Random } from "../../core/Random";
import { SectionId, SectionIds } from "../../core/model";
import { LogManager } from "../../core/Logger";
import { NjObject } from "../../core/data_formats/parsing/ninja";

const logger = LogManager.get("viewer/stores/ModelStore");

export class ModelStore extends Store {
    // Character classes and their animations.
    private readonly _current_character_class: WritableProperty<
        CharacterClassModel | undefined
    > = property(undefined);
    private readonly _current_section_id: WritableProperty<SectionId>;
    private readonly _current_body: WritableProperty<number | undefined> = property(0);
    private readonly _current_animation: WritableProperty<
        CharacterClassAnimationModel | undefined
    > = property(undefined);

    // Geometry, textures and animations.
    private readonly _current_nj_object = property<NjObject | undefined>(undefined);
    private readonly _current_textures = list_property<XvrTexture | undefined>();
    private readonly _current_nj_motion = property<NjMotion | undefined>(undefined);

    // User settings.
    private readonly _show_skeleton: WritableProperty<boolean> = property(false);
    private readonly _animation_playing: WritableProperty<boolean> = property(true);
    private readonly _animation_frame_rate: WritableProperty<number> = property(PSO_FRAME_RATE);
    private readonly _animation_frame: WritableProperty<number> = property(0);

    // Character classes and their animations.
    readonly character_classes: readonly CharacterClassModel[] = [
        HUMAR,
        HUNEWEARL,
        HUCAST,
        HUCASEAL,
        RAMAR,
        RAMARL,
        RACAST,
        RACASEAL,
        FOMAR,
        FOMARL,
        FONEWM,
        FONEWEARL,
    ];
    readonly current_character_class: Property<CharacterClassModel | undefined> = this
        ._current_character_class;
    readonly current_section_id: Property<SectionId>;
    readonly current_body: Property<number | undefined> = this._current_body;
    readonly animations: readonly CharacterClassAnimationModel[] = new Array(572)
        .fill(undefined)
        .map((_, i) => new CharacterClassAnimationModel(i, `Animation ${i + 1}`));
    readonly current_animation: Property<CharacterClassAnimationModel | undefined> = this
        ._current_animation;

    // Geometry, textures and animations.
    readonly current_nj_object: Property<NjObject | undefined> = this._current_nj_object;
    readonly current_textures: ListProperty<XvrTexture | undefined> = this._current_textures;
    readonly current_nj_motion: Property<NjMotion | undefined> = this._current_nj_motion;
    readonly animation_frame_count: Property<number> = this.current_nj_motion.map(njm =>
        njm ? njm.frame_count : 0,
    );

    // User settings.
    readonly show_skeleton: Property<boolean> = this._show_skeleton;
    readonly animation_playing: Property<boolean> = this._animation_playing;
    readonly animation_frame_rate: Property<number> = this._animation_frame_rate;
    readonly animation_frame: Property<number> = this._animation_frame;

    constructor(
        private readonly asset_loader: CharacterClassAssetLoader,
        private readonly random: Random,
    ) {
        super();

        this._current_section_id = property(random.sample_array(SectionIds));
        this.current_section_id = this._current_section_id;

        this.disposables(
            this.current_character_class.observe(this.load_character_class_model),
            this.current_section_id.observe(this.load_character_class_model),
            this.current_body.observe(this.load_character_class_model),
            this.current_animation.observe(this.load_animation),
        );

        const character_class = random.sample_array(this.character_classes);
        this.set_current_character_class(character_class);
    }

    set_current_character_class = (character_class?: CharacterClassModel): void => {
        if (this._current_character_class.val !== character_class) {
            this._current_character_class.val = character_class;
            this.set_current_body(
                character_class
                    ? this.random.integer(0, character_class.body_style_count)
                    : undefined,
            );

            if (this.current_animation.val == undefined) {
                this.set_current_nj_motion(undefined);
            }
        }
    };

    set_current_section_id = (section_id: SectionId): void => {
        this._current_section_id.val = section_id;
    };

    set_current_body = (body?: number): void => {
        this._current_body.val = body;
    };

    set_current_animation = (animation?: CharacterClassAnimationModel): void => {
        if (this._current_animation.val !== animation) {
            this._current_animation.val = animation;
        }
    };

    set_current_nj_object = (nj_object?: NjObject): void => {
        this.set_current_character_class(undefined);
        this.set_current_animation(undefined);
        this.set_current_textures([]);
        this.set_current_nj_motion(undefined);
        this._current_nj_object.val = nj_object;
    };

    set_current_textures = (textures: (XvrTexture | undefined)[]): void => {
        this._current_textures.val = textures;
    };

    set_current_nj_motion = (nj_motion?: NjMotion): void => {
        this.set_current_animation(undefined);
        this._current_nj_motion.val = nj_motion;
    };

    set_show_skeleton = (show_skeleton: boolean): void => {
        this._show_skeleton.val = show_skeleton;
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

    private load_character_class_model = async (): Promise<void> => {
        const character_class = this.current_character_class.val;
        if (character_class == undefined) return;

        const body = this.current_body.val;
        if (body == undefined) return;

        try {
            this._current_nj_object.val = undefined;

            const nj_object = await this.asset_loader.load_geometry(character_class);

            this._current_textures.val = await this.asset_loader.load_textures(
                character_class,
                this.current_section_id.val,
                body,
            );

            this._current_nj_object.val = nj_object;
        } catch (e) {
            logger.error(`Couldn't load model for ${character_class.name}.`);
            this._current_nj_object.val = undefined;
        }
    };

    private load_animation = async (): Promise<void> => {
        const nj_object = this._current_nj_object.val;
        const animation = this.current_animation.val;

        if (nj_object && animation) {
            try {
                this._current_nj_motion.val = await this.asset_loader.load_animation(
                    animation.id,
                    64,
                );
                this.set_animation_playing(true);
            } catch (e) {
                logger.error(`Couldn't load animation "${animation.name}".`, e);
                this._current_nj_motion.val = undefined;
            }
        } else {
            this._current_nj_motion.val = undefined;
        }
    };
}
