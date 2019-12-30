import { Controller } from "../../core/controllers/Controller";
import { QuestEditorStore } from "../stores/QuestEditorStore";
import { Property } from "../../core/observable/property/Property";
import { QuestEventDagModel } from "../model/QuestEventDagModel";
import { ListProperty } from "../../core/observable/property/list/ListProperty";
import { flat_map_to_list, list_property, map } from "../../core/observable";
import { QuestEventModel } from "../model/QuestEventModel";
import { EditEventDelayAction } from "../actions/EditEventDelayAction";
import { WaveModel } from "../model/WaveModel";
import { RemoveEventAction } from "../actions/RemoveEventAction";

export class EventsController extends Controller {
    readonly event_dag: Property<QuestEventDagModel | undefined>;
    readonly event_sub_graphs: ListProperty<ListProperty<QuestEventModel>>;
    readonly enabled: Property<boolean>;
    readonly unavailable: Property<boolean>;

    constructor(private readonly store: QuestEditorStore) {
        super();

        this.enabled = store.quest_runner.running.map(r => !r);
        this.unavailable = store.current_quest.map(q => q == undefined);

        this.event_dag = map(
            (quest, area) => {
                if (quest && area) {
                    return quest.event_dags.get(area.id);
                } else {
                    return undefined;
                }
            },
            store.current_quest,
            store.current_area,
        );

        this.event_sub_graphs = flat_map_to_list(
            dag => dag?.connected_sub_graphs ?? list_property(),
            this.event_dag,
        );
    }

    focused = (): void => {
        this.store.undo.make_current();
    };

    is_selected(event: QuestEventModel): Property<boolean> {
        return this.store.selected_wave.map(selected => event.wave === selected);
    }

    add_event = (): void => {
        const quest = this.store.current_quest.val;
        const area = this.store.current_area.val;

        if (quest && area) {
            const section_id = this.store.selected_entity.val?.section_id?.val ?? 1;

            const event_ids: number[] = [];
            const wave_ids: number[] = [];

            for (const sub_graph of this.event_sub_graphs) {
                for (const event of sub_graph) {
                    if (event.wave.area_id.val === area.id) {
                        event_ids.push(event.id);

                        if (event.wave.section_id.val === section_id) {
                            wave_ids.push(event.wave.id.val);
                        }
                    }
                }
            }

            event_ids.sort((a, b) => a - b);
            wave_ids.sort((a, b) => a - b);

            // Find the first available wave id.
            let wave_id: number = 0;

            for (const existing_wave_id of wave_ids) {
                if (++wave_id !== existing_wave_id) {
                    break;
                }
            }

            if (wave_id === wave_ids.length) {
                wave_id++;
            }

            // Create id based on section id and wave id.
            const id_str = `${section_id}${wave_id}`;
            let id = parseInt(id_str, 10);

            // Make sure id is unique.
            let existing_index: number = 0;

            while (true) {
                existing_index = event_ids.indexOf(id, existing_index);

                if (existing_index === -1) {
                    break;
                } else {
                    id++;
                }
            }

            quest.add_event(
                new QuestEventModel(
                    id,
                    section_id,
                    new WaveModel(wave_id, area.id, section_id),
                    30,
                    0, // TODO: what is a sensible value for event.unknown?
                ),
                [],
                [],
            );
        }
    };

    remove_event = (event: QuestEventModel): void => {
        const quest = this.store.current_quest.val;
        const dag = this.event_dag.val;

        if (quest && dag) {
            this.store.undo.push(new RemoveEventAction(this.store, quest, dag, event)).redo();
        }
    };

    set_selected_wave = (wave?: WaveModel): void => {
        if (this.enabled.val) {
            this.store.set_selected_wave(wave);
        }
    };

    set_delay = (event: QuestEventModel, delay: number): void => {
        this.store.undo.push(new EditEventDelayAction(event, event.delay.val, delay)).redo();
    };
}
