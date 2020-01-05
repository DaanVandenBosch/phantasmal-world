import { Controller } from "../../../core/controllers/Controller";
import { ModelStore } from "../../stores/ModelStore";
import { CharacterClassModel } from "../../model/CharacterClassModel";
import { Property } from "../../../core/observable/property/Property";
import { CharacterClassAnimationModel } from "../../model/CharacterClassAnimationModel";

export class ModelController extends Controller {
    readonly character_classes: readonly CharacterClassModel[];
    readonly current_character_class: Property<CharacterClassModel | undefined>;

    readonly animations: readonly CharacterClassAnimationModel[];
    readonly current_animation: Property<CharacterClassAnimationModel | undefined>;

    constructor(private readonly store: ModelStore) {
        super();

        this.character_classes = store.character_classes;
        this.current_character_class = store.current_character_class;

        this.animations = store.animations;
        this.current_animation = store.current_animation;
    }

    set_current_character_class = (character_class: CharacterClassModel): void => {
        this.store.set_current_character_class(character_class);
    };

    set_current_animation = (animation: CharacterClassAnimationModel): void => {
        this.store.set_current_animation(animation);
    };
}
