import { QuestModel } from "../model/QuestModel";
import { QuestObjectModel } from "../model/QuestObjectModel";
import { QuestNpcModel } from "../model/QuestNpcModel";
import { QuestEventModel } from "../model/QuestEventModel";
import {
    DatEvent,
    DatEventAction,
    DatEventActionType,
} from "../../core/data_formats/parsing/quest/dat";
import {
    QuestEventActionLockModel,
    QuestEventActionSpawnNpcsModel,
    QuestEventActionType,
    QuestEventActionUnlockModel,
} from "../model/QuestEventActionModel";
import { QuestEventDagModel } from "../model/QuestEventDagModel";
import { Quest, QuestEvent } from "../../core/data_formats/parsing/quest/Quest";
import { clone_segment } from "../../core/data_formats/asm/instructions";
import { AreaStore } from "./AreaStore";
import { LogManager } from "../../core/Logger";
import { WaveModel } from "../model/WaveModel";
import {
    get_npc_section_id,
    get_npc_wave,
    QuestNpc,
} from "../../core/data_formats/parsing/quest/QuestNpc";

const logger = LogManager.get("quest_editor/stores/model_conversion");

export function convert_quest_to_model(area_store: AreaStore, quest: Quest): QuestModel {
    const wave_cache = new Map<string, WaveModel>();

    return new QuestModel(
        area_store,
        quest.id,
        quest.language,
        quest.name,
        quest.short_description,
        quest.long_description,
        quest.episode,
        quest.map_designations,
        quest.objects.map(obj => new QuestObjectModel(obj)),
        quest.npcs.map(npc => convert_npc_to_model(wave_cache, npc)),
        build_event_dags(wave_cache, quest.events),
        quest.dat_unknowns,
        quest.object_code.slice(),
        quest.shop_items,
    );
}

function convert_npc_to_model(wave_cache: Map<string, WaveModel>, npc: QuestNpc): QuestNpcModel {
    const wave_id = get_npc_wave(npc);
    const wave =
        wave_id === 0
            ? undefined
            : get_wave(wave_cache, npc.area_id, get_npc_section_id(npc), wave_id);

    return new QuestNpcModel(npc, wave);
}

function get_wave(
    wave_cache: Map<string, WaveModel>,
    area_id: number,
    section_id: number,
    wave_id: number,
): WaveModel {
    const wave_key = `${area_id}-${section_id}-${wave_id}`;
    let wave = wave_cache.get(wave_key);

    if (!wave) {
        wave = new WaveModel(wave_id, area_id, section_id);
        wave_cache.set(wave_key, wave);
    }

    return wave;
}

function build_event_dags(
    wave_cache: Map<string, WaveModel>,
    dat_events: readonly DatEvent[],
): Map<number, QuestEventDagModel> {
    // Build up a temporary data structure with partial data.
    // Maps event id and area id to data.
    const data_map = new Map<
        string,
        {
            event?: QuestEventModel;
            area_id: number;
            parents: QuestEventModel[];
            child_ids: number[];
        }
    >();

    for (const event of dat_events) {
        const key = `${event.id}-${event.area_id}`;
        let data = data_map.get(key);

        if (data && data.event) {
            logger.warn(`Ignored duplicate event #${event.id} for area ${event.area_id}.`);
            continue;
        }

        const wave = get_wave(wave_cache, event.area_id, event.section_id, event.wave);

        const event_model = new QuestEventModel(
            event.id,
            event.section_id,
            wave,
            event.delay,
            event.unknown,
        );

        if (data) {
            data.event = event_model;
        } else {
            data = {
                event: event_model,
                area_id: event.area_id,
                parents: [],
                child_ids: [],
            };
            data_map.set(key, data);
        }

        for (const action of event.actions) {
            switch (action.type) {
                case DatEventActionType.SpawnNpcs:
                    event_model.add_action(
                        new QuestEventActionSpawnNpcsModel(action.section_id, action.appear_flag),
                    );
                    break;
                case DatEventActionType.Unlock:
                    event_model.add_action(new QuestEventActionUnlockModel(action.door_id));
                    break;
                case DatEventActionType.Lock:
                    event_model.add_action(new QuestEventActionLockModel(action.door_id));
                    break;
                case DatEventActionType.TriggerEvent:
                    {
                        data.child_ids.push(action.event_id);

                        const child_key = `${action.event_id}-${event.area_id}`;
                        const child_data = data_map.get(child_key);

                        if (child_data) {
                            child_data.parents.push(event_model);
                        } else {
                            data_map.set(child_key, {
                                area_id: event.area_id,
                                parents: [event_model],
                                child_ids: [],
                            });
                        }
                    }
                    break;
                default:
                    logger.warn(`Unknown event action type: ${(action as any).type}.`);
                    break;
            }
        }
    }

    // Convert temporary structure to DAG. Events that call nonexistent events are filtered out.

    // Maps area IDs to DAGs.
    const event_dags: Map<number, QuestEventDagModel> = new Map();

    // Add nodes to the DAG.
    for (const { area_id, event } of data_map.values()) {
        if (event) {
            let dag = event_dags.get(area_id);

            if (!dag) {
                dag = new QuestEventDagModel(area_id);
                event_dags.set(area_id, dag);
            }

            dag.add_event(event, [], []);
        }
    }

    // Add edges to the DAG.
    for (const data of data_map.values()) {
        if (data.event) {
            for (const child_id of data.child_ids) {
                const child = data_map.get(`${child_id}-${data.area_id}`)!;

                if (child.event) {
                    event_dags.get(data.area_id)!.add_edge(data.event, child.event);
                } else {
                    logger.warn(`Event ${data.event.id} calls nonexistent event ${child_id}.`);
                }
            }
        }
    }

    return event_dags;
}

export function convert_quest_from_model(quest: QuestModel): Quest {
    return {
        id: quest.id.val,
        language: quest.language.val,
        name: quest.name.val,
        short_description: quest.short_description.val,
        long_description: quest.long_description.val,
        episode: quest.episode,
        objects: quest.objects.val.map(obj => obj.entity),
        npcs: quest.npcs.val.map(npc => npc.entity),
        events: convert_quest_events_from_model(quest.event_dags),
        dat_unknowns: quest.dat_unknowns.map(unk => ({ ...unk })),
        object_code: quest.object_code.map(seg => clone_segment(seg)),
        shop_items: quest.shop_items.slice(),
        map_designations: new Map(quest.map_designations.val),
    };
}

function convert_quest_events_from_model(
    event_dags: Map<number, QuestEventDagModel>,
): QuestEvent[] {
    const events: QuestEvent[] = [];

    for (const event_dag of event_dags.values()) {
        for (const event of event_dag.events) {
            const actions: DatEventAction[] = event.actions.val.map(action => {
                switch (action.type) {
                    case QuestEventActionType.SpawnNpcs:
                        return {
                            type: DatEventActionType.SpawnNpcs,
                            section_id: action.section_id,
                            appear_flag: action.appear_flag,
                        };
                    case QuestEventActionType.Unlock:
                        return {
                            type: DatEventActionType.Unlock,
                            door_id: action.door_id.val,
                        };
                    case QuestEventActionType.Lock:
                        return {
                            type: DatEventActionType.Lock,
                            door_id: action.door_id.val,
                        };
                }
            });

            for (const child_event of event_dag.get_children(event)) {
                actions.push({
                    type: DatEventActionType.TriggerEvent,
                    event_id: child_event.id,
                });
            }

            events.push({
                id: event.id,
                section_id: event.section_id,
                wave: event.wave.id.val,
                delay: event.delay.val,
                actions,
                area_id: event_dag.area_id,
                unknown: event.unknown,
            });
        }
    }

    return events;
}
