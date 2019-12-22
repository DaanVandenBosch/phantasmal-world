import { Action } from "../../core/undo/Action";
import { QuestEventModel } from "../model/QuestEventModel";

export class EditEventDelayAction implements Action {
    readonly description: string;

    constructor(
        private readonly event: QuestEventModel,
        private readonly old_delay: number,
        private readonly new_delay: number,
    ) {
        this.description = `Edit delay of event ${event.id}`;
    }

    undo(): void {
        this.event.set_delay(this.old_delay);
    }

    redo(): void {
        this.event.set_delay(this.new_delay);
    }
}
