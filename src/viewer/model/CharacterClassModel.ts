export class CharacterClassModel {
    readonly name: string;
    readonly body_style_count: number;
    readonly head_style_count: number;
    readonly hair_style_count: number;
    readonly hair_styles_with_accessory: Set<number>;
    readonly body_tex_ids: readonly number[][];
    readonly head_tex_ids: readonly (number | undefined)[];
    readonly hair_tex_ids: readonly (number | undefined)[];
    readonly accessory_tex_ids: readonly (number | undefined)[];

    constructor(props: {
        name: string;
        body_style_count?: number;
        head_style_count: number;
        hair_style_count: number;
        hair_styles_with_accessory: Set<number>;
        body_tex_ids?: number[][];
        head_tex_ids?: (number | undefined)[];
        hair_tex_ids?: (number | undefined)[];
        accessory_tex_ids?: (number | undefined)[];
    }) {
        this.name = props.name;
        this.body_style_count = props.body_style_count ?? 1;
        this.head_style_count = props.head_style_count;
        this.hair_style_count = props.hair_style_count;
        this.hair_styles_with_accessory = props.hair_styles_with_accessory;
        this.body_tex_ids = props.body_tex_ids ?? [];
        this.head_tex_ids = props.head_tex_ids ?? [];
        this.hair_tex_ids = props.hair_tex_ids ?? [];
        this.accessory_tex_ids = props.accessory_tex_ids ?? [];
    }
}

export const HUMAR = new CharacterClassModel({
    name: "HUmar",
    body_style_count: 18,
    head_style_count: 1,
    hair_style_count: 10,
    hair_styles_with_accessory: new Set([6]),
});
export const HUNEWEARL = new CharacterClassModel({
    name: "HUnewearl",
    body_style_count: 18,
    head_style_count: 1,
    hair_style_count: 10,
    hair_styles_with_accessory: new Set(),
});
export const HUCAST = new CharacterClassModel({
    name: "HUcast",
    body_style_count: 25,
    head_style_count: 5,
    hair_style_count: 0,
    hair_styles_with_accessory: new Set(),
});
export const HUCASEAL = new CharacterClassModel({
    name: "HUcaseal",
    body_style_count: 25,
    head_style_count: 5,
    hair_style_count: 0,
    hair_styles_with_accessory: new Set(),
});
export const RAMAR = new CharacterClassModel({
    name: "RAmar",
    body_style_count: 18,
    head_style_count: 1,
    hair_style_count: 10,
    hair_styles_with_accessory: new Set([0, 1, 2, 3, 4, 5, 6, 7, 8, 9]),
});
export const RAMARL = new CharacterClassModel({
    name: "RAmarl",
    body_style_count: 18,
    head_style_count: 1,
    hair_style_count: 10,
    hair_styles_with_accessory: new Set([0, 1, 2, 3, 4, 5, 6, 7, 8, 9]),
});
export const RACAST = new CharacterClassModel({
    name: "RAcast",
    body_style_count: 25,
    head_style_count: 5,
    hair_style_count: 0,
    hair_styles_with_accessory: new Set(),
});
export const RACASEAL = new CharacterClassModel({
    name: "RAcaseal",
    body_style_count: 25,
    head_style_count: 5,
    hair_style_count: 0,
    hair_styles_with_accessory: new Set(),
});
export const FOMAR = new CharacterClassModel({
    name: "FOmar",
    body_style_count: 18,
    head_style_count: 1,
    hair_style_count: 10,
    hair_styles_with_accessory: new Set([0, 1, 2, 3, 4, 5, 6, 7, 8, 9]),
});
export const FOMARL = new CharacterClassModel({
    name: "FOmarl",
    body_style_count: 18,
    head_style_count: 1,
    hair_style_count: 10,
    hair_styles_with_accessory: new Set([0, 1, 2, 3, 4, 5, 6, 7, 8, 9]),
});
export const FONEWM = new CharacterClassModel({
    name: "FOnewm",
    body_style_count: 18,
    head_style_count: 1,
    hair_style_count: 10,
    hair_styles_with_accessory: new Set([0, 1, 2, 3, 4, 5, 6, 7, 8, 9]),
});
export const FONEWEARL = new CharacterClassModel({
    name: "FOnewearl",
    body_style_count: 18,
    head_style_count: 1,
    hair_style_count: 10,
    hair_styles_with_accessory: new Set([0, 1, 2, 3, 4, 5, 6, 7, 8, 9]),
});
