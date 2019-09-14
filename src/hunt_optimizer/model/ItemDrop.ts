import { ItemType } from "../../core/model/items";
import { Difficulty, SectionId } from "../../core/model";
import { NpcType } from "../../core/data_formats/parsing/quest/npc_types";

interface ItemDrop {
    readonly item_type: ItemType;
    readonly anything_rate: number;
    readonly rare_rate: number;
}

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
