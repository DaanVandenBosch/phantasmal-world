export type ItemTypeDto = WeaponItemTypeDto
    | ArmorItemTypeDto
    | ShieldItemTypeDto
    | UnitItemTypeDto
    | ToolItemTypeDto

export type WeaponItemTypeDto = {
    class: 'weapon',
    id: number,
    name: string,
    minAtp: number,
    maxAtp: number,
    ata: number,
    maxGrind: number,
    requiredAtp: number,
}

export type ArmorItemTypeDto = {
    class: 'armor',
    id: number,
    name: string,
}

export type ShieldItemTypeDto = {
    class: 'shield',
    id: number,
    name: string,
}

export type UnitItemTypeDto = {
    class: 'unit',
    id: number,
    name: string,
}

export type ToolItemTypeDto = {
    class: 'tool',
    id: number,
    name: string,
}

export type EnemyDropDto = {
    difficulty: string,
    episode: number,
    sectionId: string,
    enemy: string,
    itemTypeId: number,
    dropRate: number,
    rareRate: number,
}

export type BoxDropDto = {
    difficulty: string,
    episode: number,
    sectionId: string,
    areaId: number,
    itemTypeId: number,
    dropRate: number,
}
