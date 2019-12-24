import { Quest } from "../../core/data_formats/parsing/quest";
import { QuestModel } from "../model/QuestModel";
import { QuestObjectModel } from "../model/QuestObjectModel";
import { vec3_to_threejs } from "../../core/rendering/conversion";
import { Euler } from "three";
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
    QuestEventActionUnlockModel,
} from "../model/QuestEventActionModel";
import { QuestEventDagModel, QuestEventDagModelMeta } from "../model/QuestEventDagModel";
import { QuestEvent } from "../../core/data_formats/parsing/quest/entities";
import { clone_segment } from "../scripting/instructions";
import { AreaStore } from "./AreaStore";
import { LogManager } from "../../core/Logger";

const logger = LogManager.get("quest_editor/stores/model_conversion");

export function convert_quest_to_model(area_store: AreaStore, quest: Quest): QuestModel {
    // Create quest model.
    return new QuestModel(
        area_store,
        quest.id,
        quest.language,
        quest.name,
        quest.short_description,
        quest.long_description,
        quest.episode,
        quest.map_designations,
        quest.objects.map(
            obj =>
                new QuestObjectModel(
                    obj.type,
                    obj.id,
                    obj.group_id,
                    obj.area_id,
                    obj.section_id,
                    vec3_to_threejs(obj.position),
                    new Euler(obj.rotation.x, obj.rotation.y, obj.rotation.z, "ZXY"),
                    obj.properties,
                    obj.unknown,
                ),
        ),
        quest.npcs.map(
            npc =>
                new QuestNpcModel(
                    npc.type,
                    npc.pso_type_id,
                    npc.npc_id,
                    npc.wave,
                    npc.pso_wave2,
                    npc.script_label,
                    npc.pso_roaming,
                    npc.area_id,
                    npc.section_id,
                    vec3_to_threejs(npc.position),
                    new Euler(npc.rotation.x, npc.rotation.y, npc.rotation.z, "ZXY"),
                    vec3_to_threejs(npc.scale),
                    npc.unknown,
                ),
        ),
        build_event_dags(quest.events),
        quest.dat_unknowns,
        quest.object_code.slice(),
        quest.shop_items,
    );
}

function build_event_dags(dat_events: readonly DatEvent[]): QuestEventDagModel[] {
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

        const event_model = new QuestEventModel(
            event.id,
            event.section_id,
            event.wave,
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

    // Convert temporary structure to complete data structure used to build DAGs. Events that call
    // nonexistent events are filtered out. This final structure is completely sound.
    const event_to_full_data = new Map<QuestEventModel, QuestEventDagModelMeta>();
    const root_events: { event: QuestEventModel; area_id: number }[] = [];

    for (const data of data_map.values()) {
        if (data.event) {
            const children: QuestEventModel[] = [];

            for (const child_id of data.child_ids) {
                const child = data_map.get(`${child_id}-${data.area_id}`)!;

                if (child.event) {
                    children.push(child.event);
                } else {
                    logger.warn(`Event ${data.event.id} calls nonexistent event ${child_id}.`);
                }
            }

            event_to_full_data.set(data.event, {
                parents: data.parents,
                children,
            });

            if (data.parents.length === 0) {
                root_events.push({ event: data.event, area_id: data.area_id });
            }
        }
    }

    // Build DAGs from final complete data structure.
    const event_dags: QuestEventDagModel[] = [];

    while (root_events.length) {
        const { event, area_id } = root_events.shift()!;

        const dag_events: QuestEventModel[] = [];
        const dag_root_events: QuestEventModel[] = [];
        const dag_meta: Map<QuestEventModel, QuestEventDagModelMeta> = new Map();

        // Start from a root event and find all connected events.
        find_dag_events(
            event_to_full_data,
            dag_events,
            dag_root_events,
            dag_meta,
            event,
            new Set(),
        );

        for (const event of dag_root_events) {
            const i = root_events.findIndex(r => r.event === event);

            if (i !== -1) {
                root_events.splice(i, 1);
            }
        }

        event_dags.push(new QuestEventDagModel(area_id, dag_events, dag_root_events, dag_meta));
    }

    return event_dags;
}

function find_dag_events(
    full_data: Map<QuestEventModel, QuestEventDagModelMeta>,
    dag_events: QuestEventModel[],
    dag_root_events: QuestEventModel[],
    dag_meta: Map<QuestEventModel, QuestEventDagModelMeta>,
    event: QuestEventModel,
    visited: Set<QuestEventModel>,
): void {
    if (visited.has(event)) return;

    visited.add(event);

    const data = full_data.get(event)!;

    dag_events.push(event);
    dag_meta.set(event, data);

    if (data.parents.length === 0) {
        dag_root_events.push(event);
    }

    for (const parent of data.parents) {
        find_dag_events(full_data, dag_events, dag_root_events, dag_meta, parent, visited);
    }

    for (const child of data.children) {
        find_dag_events(full_data, dag_events, dag_root_events, dag_meta, child, visited);
    }
}

export function convert_quest_from_model(quest: QuestModel): Quest {
    return {
        id: quest.id.val,
        language: quest.language.val,
        name: quest.name.val,
        short_description: quest.short_description.val,
        long_description: quest.long_description.val,
        episode: quest.episode,
        objects: quest.objects.val.map(obj => ({
            type: obj.type,
            area_id: obj.area_id,
            section_id: obj.section_id.val,
            position: obj.position.val.clone(),
            rotation: obj.rotation.val.clone(),
            unknown: obj.unknown,
            id: obj.id,
            group_id: obj.group_id,
            properties: new Map(obj.properties),
        })),
        npcs: quest.npcs.val.map(npc => ({
            type: npc.type,
            area_id: npc.area_id,
            section_id: npc.section_id.val,
            wave: npc.wave.val,
            pso_wave2: npc.pso_wave2,
            position: npc.position.val.clone(),
            rotation: npc.rotation.val.clone(),
            scale: npc.scale.clone(),
            unknown: npc.unknown,
            pso_type_id: npc.pso_type_id,
            npc_id: npc.npc_id,
            script_label: npc.script_label,
            pso_roaming: npc.pso_roaming,
        })),
        events: convert_quest_events_from_model(quest.event_dags.val),
        dat_unknowns: quest.dat_unknowns.map(unk => ({ ...unk })),
        object_code: quest.object_code.map(seg => clone_segment(seg)),
        shop_items: quest.shop_items.slice(),
        map_designations: new Map(quest.map_designations.val),
    };
}

function convert_quest_events_from_model(event_dags: readonly QuestEventDagModel[]): QuestEvent[] {
    const events: QuestEvent[] = [];

    for (const event_dag of event_dags) {
        for (const event of event_dag.events) {
            const actions: DatEventAction[] = event.actions.val.map(action => {
                if (action instanceof QuestEventActionSpawnNpcsModel) {
                    return {
                        type: DatEventActionType.SpawnNpcs,
                        section_id: action.section_id,
                        appear_flag: action.appear_flag,
                    };
                } else if (action instanceof QuestEventActionUnlockModel) {
                    return {
                        type: DatEventActionType.Unlock,
                        door_id: action.door_id,
                    };
                } else if (action instanceof QuestEventActionLockModel) {
                    return {
                        type: DatEventActionType.Lock,
                        door_id: action.door_id,
                    };
                } else {
                    throw new Error(
                        `Unknown event action type ${Object.getPrototypeOf(action).constructor}`,
                    );
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
                section_id: event.section_id.val,
                wave: event.wave,
                delay: event.delay.val,
                actions,
                area_id: event_dag.area_id,
                unknown: event.unknown,
            });
        }
    }

    return events;
}
