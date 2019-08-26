import { QuestEditAction } from "./QuestEditAction";

export class EditNameAction extends QuestEditAction<string> {
    readonly description = "Edit name";

    undo(): void {
        this.quest.set_name(this.old);
    }

    redo(): void {
        this.quest.set_name(this.new);
    }
}
