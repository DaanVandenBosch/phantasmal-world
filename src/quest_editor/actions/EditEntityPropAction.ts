import { Action } from "../../core/undo/Action";
import { QuestEntityPropModel } from "../model/QuestEntityPropModel";
import { QuestEditorStore } from "../stores/QuestEditorStore";
import { QuestEntityModel } from "../model/QuestEntityModel";

export class EditEntityPropAction implements Action {
    readonly description: string;

    constructor(
        private readonly quest_editor_store: QuestEditorStore,
        private readonly entity: QuestEntityModel,
        private readonly prop: QuestEntityPropModel,
        private readonly old_value: number,
        private readonly new_value: number,
    ) {
        this.description = `Edit ${prop.name}`;
    }

    redo(): void {
        this.prop.set_value(this.new_value);
        this.quest_editor_store.set_selected_entity(this.entity);
    }

    undo(): void {
        this.prop.set_value(this.old_value);
        this.quest_editor_store.set_selected_entity(this.entity);
    }
}
