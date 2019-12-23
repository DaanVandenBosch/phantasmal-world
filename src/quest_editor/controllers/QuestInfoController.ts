import { Property, PropertyChangeEvent } from "../../core/observable/property/Property";
import { EditIdAction } from "../actions/EditIdAction";
import { EditNameAction } from "../actions/EditNameAction";
import { EditShortDescriptionAction } from "../actions/EditShortDescriptionAction";
import { EditLongDescriptionAction } from "../actions/EditLongDescriptionAction";
import { QuestModel } from "../model/QuestModel";
import { QuestEditorStore } from "../stores/QuestEditorStore";

export class QuestInfoController {
    readonly current_quest: Property<QuestModel | undefined>;
    readonly enabled: Property<boolean>;
    readonly unavailable: Property<boolean>;

    constructor(private readonly store: QuestEditorStore) {
        this.current_quest = store.current_quest;
        this.enabled = store.quest_runner.running.map(r => !r);
        this.unavailable = this.current_quest.map(q => q == undefined);
    }

    focused = (): void => {
        this.store.undo.make_current();
    };

    id_changed = (event: PropertyChangeEvent<number>): void => {
        if (this.current_quest.val) {
            this.store.undo.push(new EditIdAction(this.current_quest.val, event)).redo();
        }
    };

    name_changed = (event: PropertyChangeEvent<string>): void => {
        if (this.current_quest.val) {
            this.store.undo.push(new EditNameAction(this.current_quest.val, event)).redo();
        }
    };

    short_description_changed = (event: PropertyChangeEvent<string>): void => {
        if (this.current_quest.val) {
            this.store.undo
                .push(new EditShortDescriptionAction(this.current_quest.val, event))
                .redo();
        }
    };

    long_description_changed = (event: PropertyChangeEvent<string>): void => {
        if (this.current_quest.val) {
            this.store.undo
                .push(new EditLongDescriptionAction(this.current_quest.val, event))
                .redo();
        }
    };
}
