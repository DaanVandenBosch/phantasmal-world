import { SectionIds } from "../../core/model";
import { create_array } from "../../core/util";

export class CharacterClassModel {
    readonly name: string;
    readonly head_style_count: number;
    readonly hair_style_count: number;
    readonly hair_styles_with_accessory: Set<number>;
    /**
     * Can be indexed with {@link SectionId}
     */
    readonly section_id_tex_ids: readonly number[];
    readonly body_tex_ids: readonly number[];
    readonly head_tex_ids: readonly (number | undefined)[];
    readonly hair_tex_ids: readonly (number | undefined)[];
    readonly accessory_tex_ids: readonly (number | undefined)[];

    constructor(props: {
        name: string;
        head_style_count: number;
        hair_style_count: number;
        hair_styles_with_accessory: Set<number>;
        section_id_tex_id: number;
        body_tex_ids: number[];
        head_tex_ids?: (number | undefined)[];
        hair_tex_ids?: (number | undefined)[];
        accessory_tex_ids?: (number | undefined)[];
    }) {
        this.name = props.name;
        this.head_style_count = props.head_style_count;
        this.hair_style_count = props.hair_style_count;
        this.hair_styles_with_accessory = props.hair_styles_with_accessory;
        this.section_id_tex_ids = create_array(SectionIds.length, i => props.section_id_tex_id + i);
        this.body_tex_ids = props.body_tex_ids;
        this.head_tex_ids = props.head_tex_ids ?? [];
        this.hair_tex_ids = props.hair_tex_ids ?? [];
        this.accessory_tex_ids = props.accessory_tex_ids ?? [];
    }
}
