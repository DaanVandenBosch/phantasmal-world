import { EntityListView } from "./EntityListView";
import { npc_data, NPC_TYPES, NpcType } from "../../core/data_formats/parsing/quest/npc_types";
import { QuestEditorStore } from "../stores/QuestEditorStore";
import { Episode } from "../../core/data_formats/parsing/quest/Episode";
import { EntityImageRenderer } from "../rendering/EntityImageRenderer";

export class NpcListView extends EntityListView<NpcType> {
    constructor(
        private readonly quest_editor_store: QuestEditorStore,
        entity_image_renderer: EntityImageRenderer,
    ) {
        super(quest_editor_store, entity_image_renderer, "quest_editor_NpcListView");

        this.disposables(
            quest_editor_store.current_quest.observe(this.filter_npcs),
            quest_editor_store.current_area.observe(this.filter_npcs),
        );

        this.filter_npcs();
        this.finalize_construction(NpcListView);
    }

    private filter_npcs = (): void => {
        const quest = this.quest_editor_store.current_quest.val;
        const area = this.quest_editor_store.current_area.val;

        const episode = quest ? quest.episode : Episode.I;
        const area_id = area ? area.id : 0;

        this.entities.val = NPC_TYPES.filter(npc => {
            const data = npc_data(npc);
            return (
                (data.episode == undefined || data.episode === episode) &&
                data.area_ids.includes(area_id)
            );
        });
    };
}
