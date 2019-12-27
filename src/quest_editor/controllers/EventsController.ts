import { Controller } from "../../core/controllers/Controller";
import { QuestEditorStore } from "../stores/QuestEditorStore";
import { Property } from "../../core/observable/property/Property";
import { QuestEventDagModel } from "../model/QuestEventDagModel";
import { ListProperty } from "../../core/observable/property/list/ListProperty";
import { flat_map_to_list, list_property } from "../../core/observable";
import { QuestEventModel } from "../model/QuestEventModel";
import { EditEventSectionIdAction } from "../actions/EditEventSectionIdAction";
import { EditEventDelayAction } from "../actions/EditEventDelayAction";
import { WaveModel } from "../model/WaveModel";

export class EventsController extends Controller {
    readonly event_dags: ListProperty<QuestEventDagModel>;
    readonly enabled: Property<boolean>;
    readonly unavailable: Property<boolean>;

    constructor(private readonly store: QuestEditorStore) {
        super();

        this.enabled = store.quest_runner.running.map(r => !r);
        this.unavailable = store.current_quest.map(q => q == undefined);

        this.event_dags = flat_map_to_list(
            (quest, area) => {
                if (quest && area) {
                    return quest.event_dags.filtered(dag => dag.area_id === area.id);
                } else {
                    return list_property();
                }
            },
            store.current_quest,
            store.current_area,
        );
    }

    focused = (): void => {
        this.store.undo.make_current();
    };

    set_section_id = (event: QuestEventModel, section_id: number): void => {
        this.store.undo
            .push(new EditEventSectionIdAction(event, event.section_id.val, section_id))
            .redo();
    };

    toggle_current_wave = (wave: WaveModel): void => {
        if (this.store.current_wave.val === wave) {
            this.store.set_current_wave(undefined);
        } else {
            this.store.set_current_wave(wave);
        }
    };

    set_delay = (event: QuestEventModel, delay: number): void => {
        this.store.undo.push(new EditEventDelayAction(event, event.delay.val, delay)).redo();
    };
}
