export type EnemyDropDto = {
    difficulty: string;
    episode: number;
    sectionId: string;
    enemy: string;
    itemTypeId: number;
    dropRate: number;
    rareRate: number;
};

export type BoxDropDto = {
    difficulty: string;
    episode: number;
    sectionId: string;
    areaId: number;
    itemTypeId: number;
    dropRate: number;
};
