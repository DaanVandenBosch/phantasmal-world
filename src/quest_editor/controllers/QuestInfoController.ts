import { Property } from "../../core/observable/property/Property";
import { EditIdAction } from "../actions/EditIdAction";
import { EditNameAction } from "../actions/EditNameAction";
import { EditShortDescriptionAction } from "../actions/EditShortDescriptionAction";
import { EditLongDescriptionAction } from "../actions/EditLongDescriptionAction";
import { QuestModel } from "../model/QuestModel";
import { QuestEditorStore } from "../stores/QuestEditorStore";
import { Controller } from "../../core/controllers/Controller";

export class QuestInfoController extends Controller {
    readonly current_quest: Property<QuestModel | undefined>;
    readonly enabled: Property<boolean>;
    readonly unavailable: Property<boolean>;

    constructor(private readonly store: QuestEditorStore) {
        super();

        this.current_quest = store.current_quest;
        this.enabled = store.quest_runner.running.map(r => !r);
        this.unavailable = this.current_quest.map(q => q == undefined);
    }

    focused = (): void => {
        this.store.undo.make_current();
    };

    set_id = (id: number): void => {
        const quest = this.current_quest.val;

        if (quest) {
            this.store.undo.push(new EditIdAction(quest, quest.id.val, id)).redo();
        }
    };

    set_name = (name: string): void => {
        const quest = this.current_quest.val;

        if (quest) {
            this.store.undo.push(new EditNameAction(quest, quest.name.val, name)).redo();
        }
    };

    set_short_description = (short_description: string): void => {
        const quest = this.current_quest.val;

        if (quest) {
            this.store.undo
                .push(
                    new EditShortDescriptionAction(
                        quest,
                        quest.short_description.val,
                        short_description,
                    ),
                )
                .redo();
        }
    };

    set_long_description = (long_description: string): void => {
        const quest = this.current_quest.val;

        if (quest) {
            this.store.undo
                .push(
                    new EditLongDescriptionAction(
                        quest,
                        quest.long_description.val,
                        long_description,
                    ),
                )
                .redo();
        }
    };
}
