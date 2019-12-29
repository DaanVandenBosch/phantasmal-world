import { Action } from "../../core/undo/Action";
import { QuestEntityModel } from "../model/QuestEntityModel";
import { entity_data } from "../../core/data_formats/parsing/quest/entities";
import { QuestModel } from "../model/QuestModel";
import { QuestEditorStore } from "../stores/QuestEditorStore";

export class CreateEntityAction implements Action {
    readonly description: string;

    constructor(
        private readonly quest_editor_store: QuestEditorStore,
        private readonly quest: QuestModel,
        private readonly entity: QuestEntityModel,
    ) {
        this.description = `Create ${entity_data(entity.type).name}`;
    }

    undo(): void {
        this.quest.remove_entity(this.entity);
    }

    redo(): void {
        this.quest.add_entity(this.entity);

        this.quest_editor_store.set_selected_entity(this.entity);
    }
}
