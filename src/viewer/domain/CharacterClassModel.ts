export class CharacterClassModel {
    constructor(
        readonly name: string,
        readonly head_style_count: number,
        readonly hair_styles_count: number,
        readonly hair_styles_with_accessory: Set<number>,
    ) {}
}
