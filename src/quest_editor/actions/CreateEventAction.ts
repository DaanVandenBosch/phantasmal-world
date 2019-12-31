import { Action } from "../../core/undo/Action";
import { QuestModel } from "../model/QuestModel";
import { QuestEventDagModel } from "../model/QuestEventDagModel";
import { QuestEventModel } from "../model/QuestEventModel";

export class CreateEventAction implements Action {
    readonly description: string;

    constructor(
        private readonly quest: QuestModel,
        private readonly event_dag: QuestEventDagModel,
        private readonly event: QuestEventModel,
        private readonly parent_event?: QuestEventModel,
    ) {
        this.description = `Add event ${event.id}`;
    }

    undo(): void {
        this.quest.remove_event(this.event_dag, this.event);
    }

    redo(): void {
        this.quest.add_event(this.event, this.parent_event ? [this.parent_event] : [], []);
    }
}
