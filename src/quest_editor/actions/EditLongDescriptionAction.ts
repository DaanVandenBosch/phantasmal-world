import { QuestEditAction } from "./QuestEditAction";

export class EditLongDescriptionAction extends QuestEditAction<string> {
    readonly description = "Edit long description";

    undo(): void {
        this.quest.set_long_description(this.old);
    }

    redo(): void {
        this.quest.set_long_description(this.new);
    }
}
