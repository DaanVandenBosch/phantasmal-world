import { QuestEditAction } from "./QuestEditAction";

export class EditShortDescriptionAction extends QuestEditAction<string> {
    readonly description = "Edit short description";

    undo(): void {
        this.quest.set_short_description(this.old);
    }

    redo(): void {
        this.quest.set_short_description(this.new);
    }
}
