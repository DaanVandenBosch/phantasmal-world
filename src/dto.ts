import { Difficulty, SectionId } from "./domain";

export type ItemDto = {
    name: string,
}

export type EnemyDropDto = {
    difficulty: Difficulty,
    episode: number,
    sectionId: SectionId,
    enemy: string,
    item: string,
    dropRate: number,
    rareRate: number,
}

export type BoxDropDto = {
    difficulty: Difficulty,
    episode: number,
    sectionId: SectionId,
    box: string,
    item: string,
    dropRate: number,
}
