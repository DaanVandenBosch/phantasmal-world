import { QuestEditAction } from "./QuestEditAction";

export class EditIdAction extends QuestEditAction<number> {
    readonly description = "Edit ID";

    undo(): void {
        this.quest.set_id(this.old);
    }

    redo(): void {
        this.quest.set_id(this.new);
    }
}
