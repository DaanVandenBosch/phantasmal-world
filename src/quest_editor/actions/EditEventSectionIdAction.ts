import { Action } from "../../core/undo/Action";
import { QuestEventModel } from "../model/QuestEventModel";

export class EditEventSectionIdAction implements Action {
    readonly description: string;

    constructor(
        private readonly event: QuestEventModel,
        private readonly old_section_id: number,
        private readonly new_section_id: number,
    ) {
        this.description = `Edit section ID of event ${event.id}`;
    }

    undo(): void {
        this.event.set_section_id(this.old_section_id);
    }

    redo(): void {
        this.event.set_section_id(this.new_section_id);
    }
}
