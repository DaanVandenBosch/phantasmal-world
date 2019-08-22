import { ObservableQuestEntity } from "./ObservableQuestEntity";
import { NpcType } from "../../core/data_formats/parsing/quest/npc_types";

export class ObservableQuestNpc extends ObservableQuestEntity<NpcType> {
    constructor(type: NpcType) {
        super(type);
    }
}
