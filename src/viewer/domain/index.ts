export class PlayerModel {
    constructor(
        readonly name: string,
        readonly head_style_count: number,
        readonly hair_styles_count: number,
        readonly hair_styles_with_accessory: Set<number>,
    ) {}
}

export class PlayerAnimation {
    constructor(readonly id: number, readonly name: string) {}
}
