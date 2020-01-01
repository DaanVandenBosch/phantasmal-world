export type EnemyDropDto = {
    difficulty: string;
    episode: number;
    section_id: string;
    enemy: string;
    item_type_id: number;
    drop_rate: number;
    rare_rate: number;
};

export type BoxDropDto = {
    difficulty: string;
    episode: number;
    section_id: string;
    area_id: number;
    item_type_id: number;
    drop_rate: number;
};
