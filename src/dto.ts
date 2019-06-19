export type ItemKindDto = WeaponItemKindDto
    | ArmorItemKindDto
    | ShieldItemKindDto
    | UnitItemKindDto
    | ToolItemKindDto

export type WeaponItemKindDto = {
    type: 'weapon',
    id: number,
    name: string,
    minAtp: number,
    maxAtp: number,
    ata: number,
    maxGrind: number,
    requiredAtp: number,
}

export type ArmorItemKindDto = {
    type: 'armor',
    id: number,
    name: string,
}

export type ShieldItemKindDto = {
    type: 'shield',
    id: number,
    name: string,
}

export type UnitItemKindDto = {
    type: 'unit',
    id: number,
    name: string,
}

export type ToolItemKindDto = {
    type: 'tool',
    id: number,
    name: string,
}

export type EnemyDropDto = {
    difficulty: string,
    episode: number,
    sectionId: string,
    enemy: string,
    itemKindId: number,
    dropRate: number,
    rareRate: number,
}

export type BoxDropDto = {
    difficulty: string,
    episode: number,
    sectionId: string,
    areaId: number,
    itemKindId: number,
    dropRate: number,
}
