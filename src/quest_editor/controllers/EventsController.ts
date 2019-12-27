import { Controller } from "../../core/controllers/Controller";
import { QuestEditorStore } from "../stores/QuestEditorStore";
import { Property } from "../../core/observable/property/Property";
import { QuestEventDagModel } from "../model/QuestEventDagModel";
import { ListProperty } from "../../core/observable/property/list/ListProperty";
import { flat_map_to_list, list_property } from "../../core/observable";
import { QuestEventModel } from "../model/QuestEventModel";
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

    add_event = (): void => {
        const quest = this.store.current_quest.val;
        const area = this.store.current_area.val;

        if (quest && area) {
            const section_id = this.store.selected_entity.val?.section_id?.val ?? 1;

            const event_ids: number[] = [];
            const wave_ids: number[] = [];

            for (const dag of quest.event_dags) {
                for (const event of dag.events) {
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
            let wave_id: number = wave_ids.length === 0 ? 1 : 0;

            for (const existing_wave_id of wave_ids) {
                if (++wave_id !== existing_wave_id) {
                    break;
                }
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

            const event = new QuestEventModel(
                id,
                section_id,
                new WaveModel(wave_id, area.id, section_id),
                30,
                0, // TODO: what is a sensible value for event.unknown?
            );

            quest.add_event_dag(
                new QuestEventDagModel(
                    area.id,
                    [event],
                    [event],
                    new Map([[event, { parents: [], children: [] }]]),
                ),
            );
        }
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
