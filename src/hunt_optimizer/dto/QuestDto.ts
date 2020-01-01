export type QuestDto = {
    id: number;
    name: string;
    episode: 1 | 2 | 4;
    enemy_counts: { [npc_type_code: string]: number };
};
