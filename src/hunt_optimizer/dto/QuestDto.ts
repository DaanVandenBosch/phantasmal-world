export type QuestDto = {
    id: number;
    name: string;
    episode: 1 | 2 | 4;
    enemyCounts: { [npcTypeCode: string]: number };
};
