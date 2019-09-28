import { Action } from "../../core/undo/Action";
import { QuestEntityModel } from "../model/QuestEntityModel";
import { entity_data } from "../../core/data_formats/parsing/quest/entities";
import { quest_editor_store } from "../stores/QuestEditorStore";
import { Euler } from "three";

export class RotateEntityAction implements Action {
    readonly description: string;

    constructor(
        private entity: QuestEntityModel,
        private old_rotation: Euler,
        private new_rotation: Euler,
        private world: boolean,
    ) {
        this.description = `Rotate ${entity_data(entity.type).name}`;
    }

    undo(): void {
        quest_editor_store.set_selected_entity(this.entity);

        if (this.world) {
            this.entity.set_world_rotation(this.old_rotation);
        } else {
            this.entity.set_rotation(this.old_rotation);
        }
    }

    redo(): void {
        quest_editor_store.set_selected_entity(this.entity);

        if (this.world) {
            this.entity.set_world_rotation(this.new_rotation);
        } else {
            this.entity.set_rotation(this.new_rotation);
        }
    }
}
