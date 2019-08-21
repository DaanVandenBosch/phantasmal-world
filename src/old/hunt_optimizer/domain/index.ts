import { Episode } from "../../../core/data_formats/parsing/quest/Episode";
import { NpcType } from "../../../core/data_formats/parsing/quest/npc_types";
import { computed, observable } from "mobx";
import { ItemType } from "../../core/domain/items";
import { Difficulty, SectionId } from "../../core/domain";

export class HuntMethod {
    readonly id: string;
    readonly name: string;
    readonly episode: Episode;
    readonly quest: SimpleQuest;
    readonly enemy_counts: Map<NpcType, number>;
    /**
     * The time it takes to complete the quest in hours.
     */
    readonly default_time: number;
    /**
     * The time it takes to complete the quest in hours as specified by the user.
     */
    @observable user_time?: number;

    @computed get time(): number {
        return this.user_time != null ? this.user_time : this.default_time;
    }

    constructor(id: string, name: string, quest: SimpleQuest, default_time: number) {
        if (!id) throw new Error("id is required.");
        if (default_time <= 0) throw new Error("default_time must be greater than zero.");
        if (!name) throw new Error("name is required.");
        if (!quest) throw new Error("quest is required.");

        this.id = id;
        this.name = name;
        this.episode = quest.episode;
        this.quest = quest;
        this.enemy_counts = quest.enemy_counts;
        this.default_time = default_time;
    }
}

export class SimpleQuest {
    constructor(
        readonly id: number,
        readonly name: string,
        readonly episode: Episode,
        readonly enemy_counts: Map<NpcType, number>,
    ) {
        if (!id) throw new Error("id is required.");
        if (!name) throw new Error("name is required.");
        if (!enemy_counts) throw new Error("enemyCounts is required.");
    }
}

type ItemDrop = {
    item_type: ItemType;
    anything_rate: number;
    rare_rate: number;
};

export class EnemyDrop implements ItemDrop {
    readonly rate: number;

    constructor(
        readonly difficulty: Difficulty,
        readonly section_id: SectionId,
        readonly npc_type: NpcType,
        readonly item_type: ItemType,
        readonly anything_rate: number,
        readonly rare_rate: number,
    ) {
        this.rate = anything_rate * rare_rate;
    }
}
