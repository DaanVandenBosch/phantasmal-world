import { Action } from "../../core/undo/Action";
import { QuestEntityModel } from "../model/QuestEntityModel";
import { Vec3 } from "../../core/data_formats/vector";
import { entity_data } from "../../core/data_formats/parsing/quest/entities";
import { quest_editor_store } from "../stores/QuestEditorStore";
import { SectionModel } from "../model/SectionModel";

export class TranslateEntityAction implements Action {
    readonly description: string;

    constructor(
        private entity: QuestEntityModel,
        private old_section: SectionModel | undefined,
        private new_section: SectionModel | undefined,
        private old_position: Vec3,
        private new_position: Vec3,
        private world: boolean,
    ) {
        this.description = `Move ${entity_data(entity.type).name}`;
    }

    undo(): void {
        quest_editor_store.set_selected_entity(this.entity);

        if (this.world) {
            this.entity.set_world_position(this.old_position);
        } else {
            this.entity.set_position(this.old_position);
        }

        if (this.old_section) {
            this.entity.set_section(this.old_section);
        }
    }

    redo(): void {
        quest_editor_store.set_selected_entity(this.entity);

        if (this.world) {
            this.entity.set_world_position(this.new_position);
        } else {
            this.entity.set_position(this.new_position);
        }

        if (this.new_section) {
            this.entity.set_section(this.new_section);
        }
    }
}
