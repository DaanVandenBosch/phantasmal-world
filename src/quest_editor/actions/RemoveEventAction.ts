import { Action } from "../../core/undo/Action";
import { QuestEventModel } from "../model/QuestEventModel";
import { QuestEventDagModel } from "../model/QuestEventDagModel";
import { QuestEditorStore } from "../stores/QuestEditorStore";
import { QuestModel } from "../model/QuestModel";

export class RemoveEventAction implements Action {
    readonly description: string;

    constructor(
        private readonly store: QuestEditorStore,
        private readonly quest: QuestModel,
        private readonly event_dag: QuestEventDagModel,
        private readonly event: QuestEventModel,
    ) {
        this.description = `Delete event ${event.id}`;
    }

    undo(): void {}

    redo(): void {
        if (this.store.selected_wave.val === this.event.wave) {
            this.store.set_selected_wave(undefined);
        }

        this.quest.remove_event(this.event_dag, this.event);
    }
}
