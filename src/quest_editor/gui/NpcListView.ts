import { EntityListView } from "./EntityListView";
import { NPC_TYPES, NpcType } from "../../core/data_formats/parsing/quest/npc_types";
import { list_property } from "../../core/observable";

export class NpcListView extends EntityListView<NpcType> {
    constructor() {
        super("quest_editor_NpcListView", list_property(undefined, ...NPC_TYPES));

        this.finalize_construction(NpcListView.prototype);
    }
}
