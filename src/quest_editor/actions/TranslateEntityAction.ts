import { Action } from "../../core/undo/Action";
import { QuestEntityModel } from "../model/QuestEntityModel";
import { entity_data } from "../../core/data_formats/parsing/quest/entities";
import { SectionModel } from "../model/SectionModel";
import { Vector3 } from "three";
import { QuestEditorStore } from "../stores/QuestEditorStore";

export class TranslateEntityAction implements Action {
    readonly description: string;

    constructor(
        private readonly quest_editor_store: QuestEditorStore,
        private readonly entity: QuestEntityModel,
        private readonly old_section: SectionModel | undefined,
        private readonly new_section: SectionModel | undefined,
        private readonly old_position: Vector3,
        private readonly new_position: Vector3,
        private readonly world: boolean,
    ) {
        this.description = `Move ${entity_data(entity.type).name}`;
    }

    undo(): void {
        this.quest_editor_store.set_selected_entity(this.entity);

        if (this.old_section) {
            this.entity.set_section(this.old_section);
        }

        if (this.world) {
            this.entity.set_world_position(this.old_position);
        } else {
            this.entity.set_position(this.old_position);
        }
    }

    redo(): void {
        this.quest_editor_store.set_selected_entity(this.entity);

        if (this.new_section) {
            this.entity.set_section(this.new_section);
        }

        if (this.world) {
            this.entity.set_world_position(this.new_position);
        } else {
            this.entity.set_position(this.new_position);
        }
    }
}
