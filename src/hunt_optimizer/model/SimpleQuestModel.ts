import { Episode } from "../../core/data_formats/parsing/quest/Episode";
import { NpcType } from "../../core/data_formats/parsing/quest/npc_types";

export class SimpleQuestModel {
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
