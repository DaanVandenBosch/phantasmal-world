import { Action } from "../../core/undo/Action";
import { QuestEntityModel } from "../model/QuestEntityModel";
import { entity_data } from "../../core/data_formats/parsing/quest/entities";
import { quest_editor_store } from "../stores/QuestEditorStore";

export class CreateEntityAction implements Action {
    readonly description: string;

    constructor(private entity: QuestEntityModel) {
        this.description = `Create ${entity_data(entity.type).name}`;
    }

    undo(): void {
        const quest = quest_editor_store.current_quest.val;

        if (quest) {
            quest.remove_entity(this.entity);
        }
    }

    redo(): void {
        const quest = quest_editor_store.current_quest.val;

        if (quest) {
            quest.add_entity(this.entity);

            quest_editor_store.set_selected_entity(this.entity);
        }
    }
}
