import { Controller } from "../../../core/controllers/Controller";
import { ModelStore } from "../../stores/ModelStore";
import { range } from "lodash";
import { Property } from "../../../core/observable/property/Property";
import { SectionId } from "../../../core/model";

export class CharacterClassOptionsController extends Controller {
    readonly enabled: Property<boolean>;
    readonly current_section_id: Property<SectionId>;
    readonly current_body_options: Property<readonly number[]>;
    readonly current_body: Property<number | undefined>;

    constructor(private readonly store: ModelStore) {
        super();

        this.enabled = store.current_character_class.map(cc => cc != undefined);

        this.current_section_id = store.current_section_id;

        this.current_body_options = store.current_character_class.map(character_class =>
            character_class ? range(1, character_class.body_style_count + 1) : [],
        );

        this.current_body = store.current_body.map(body =>
            body == undefined ? undefined : body + 1,
        );
    }

    set_current_section_id = (section_id?: SectionId): void => {
        if (section_id != undefined) {
            this.store.set_current_section_id(section_id);
        }
    };

    set_current_body = (body?: number): void => {
        this.store.set_current_body(body == undefined ? undefined : body - 1);
    };
}
