import { Action } from "../../core/undo/Action";
import { QuestEntityModel } from "../model/QuestEntityModel";
import { entity_data } from "../../core/data_formats/parsing/quest/entities";
import { QuestEditorStore } from "../stores/QuestEditorStore";
import { QuestModel } from "../model/QuestModel";

export class RemoveEntityAction implements Action {
    readonly description: string;

    constructor(
        private readonly quest_editor_store: QuestEditorStore,
        private readonly quest: QuestModel,
        private readonly entity: QuestEntityModel,
    ) {
        this.description = `Delete ${entity_data(entity.type).name}`;
        this.redo();
    }

    undo(): void {
        this.quest.add_entity(this.entity);

        this.quest_editor_store.set_selected_entity(this.entity);
    }

    redo(): void {
        this.quest.remove_entity(this.entity);
    }
}
