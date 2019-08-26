import { Action } from "../../core/undo/Action";
import { QuestEntityModel } from "../model/QuestEntityModel";
import { Vec3 } from "../../core/data_formats/vector";
import { entity_data } from "../../core/data_formats/parsing/quest/entities";
import { quest_editor_store } from "../stores/QuestEditorStore";

export class TranslateEntityAction implements Action {
    readonly description: string;

    constructor(
        private entity: QuestEntityModel,
        private old_position: Vec3,
        private new_position: Vec3,
    ) {
        this.description = `Move ${entity_data(entity.type).name}`;
    }

    undo(): void {
        this.entity.set_world_position(this.old_position);
        quest_editor_store.set_selected_entity(this.entity);
    }

    redo(): void {
        this.entity.set_world_position(this.new_position);
        quest_editor_store.set_selected_entity(this.entity);
    }
}
