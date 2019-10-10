import { Quest } from "../../core/data_formats/parsing/quest";
import { QuestModel } from "../model/QuestModel";
import { QuestObjectModel } from "../model/QuestObjectModel";
import { vec3_to_threejs } from "../../core/rendering/conversion";
import { Euler } from "three";
import { QuestNpcModel } from "../model/QuestNpcModel";
import { QuestEventModel } from "../model/QuestEventModel";
import {
    DatEventAction,
    DatEventActionTriggerEvent,
    DatEventActionType,
} from "../../core/data_formats/parsing/quest/dat";
import {
    QuestEventActionLockModel,
    QuestEventActionSpawnNpcsModel,
    QuestEventActionUnlockModel,
} from "../model/QuestEventActionModel";
import { QuestEventChainModel } from "../model/QuestEventChainModel";
import { QuestEvent } from "../../core/data_formats/parsing/quest/entities";
import Logger from "js-logger";

const logger = Logger.get("quest_editor/stores/model_conversion");

export function convert_quest_to_model(quest: Quest): QuestModel {
    // Build up event chains.
    const events = quest.events.slice();
    const event_chains: QuestEventChainModel[] = [];

    while (events.length) {
        let event: QuestEvent | undefined = events.shift();
        const chain_events = [];

        while (event) {
            chain_events.push(
                new QuestEventModel(
                    event.id,
                    event.section_id,
                    event.wave,
                    event.delay,
                    event.actions
                        .filter(action => action.type !== DatEventActionType.TriggerEvent)
                        .map(action => {
                            switch (action.type) {
                                case DatEventActionType.SpawnNpcs:
                                    return new QuestEventActionSpawnNpcsModel(
                                        action.section_id,
                                        action.appear_flag,
                                    );
                                case DatEventActionType.Unlock:
                                    return new QuestEventActionUnlockModel(action.door_id);
                                case DatEventActionType.Lock:
                                    return new QuestEventActionLockModel(action.door_id);
                                case DatEventActionType.TriggerEvent:
                                    throw new Error("Can't convert trigger event actions.");
                            }
                        }),
                    event.area_id,
                    event.unknown,
                ),
            );

            const event_id = event.id;

            const trigger_event_actions = event.actions.filter(
                action => action.type === DatEventActionType.TriggerEvent,
            ) as DatEventActionTriggerEvent[];

            event = undefined;

            if (trigger_event_actions.length >= 1) {
                if (trigger_event_actions.length > 1) {
                    logger.warn(`Event ${event_id} has more than 1 trigger event action.`);
                }

                const index = events.findIndex(e => e.id === trigger_event_actions[0].event_id);

                if (index !== -1) {
                    event = events.splice(index, 1)[0];
                }
            }
        }

        const chain = new QuestEventChainModel(chain_events);
        event_chains.push(chain);
    }

    // Create quest model.
    return new QuestModel(
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
        event_chains,
        quest.dat_unknowns,
        quest.object_code,
        quest.shop_items,
    );
}

export function convert_quest_from_model(quest: QuestModel): Quest {
    const events: QuestEvent[] = [];

    for (const chain of quest.event_chains.val) {
        for (let i = 0; i < chain.events.length.val; i++) {
            const event = chain.events.get(i);
            const next_event: QuestEventModel | undefined = chain.events.get(i + 1);

            const actions: DatEventAction[] = event.actions.map(action => {
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

            if (next_event) {
                actions.push({
                    type: DatEventActionType.TriggerEvent,
                    event_id: next_event.id,
                });
            }

            events.push({
                id: event.id,
                section_id: event.section_id,
                wave: event.wave,
                delay: event.delay,
                actions,
                area_id: event.area_id,
                unknown: event.unknown,
            });
        }
    }

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
            position: obj.position.val,
            rotation: obj.rotation.val,
            unknown: obj.unknown,
            id: obj.id,
            group_id: obj.group_id,
            properties: obj.properties,
        })),
        npcs: quest.npcs.val.map(npc => ({
            type: npc.type,
            area_id: npc.area_id,
            section_id: npc.section_id.val,
            position: npc.position.val,
            rotation: npc.rotation.val,
            scale: npc.scale,
            unknown: npc.unknown,
            pso_type_id: npc.pso_type_id,
            npc_id: npc.npc_id,
            script_label: npc.script_label,
            pso_roaming: npc.pso_roaming,
        })),
        events,
        dat_unknowns: quest.dat_unknowns,
        object_code: quest.object_code,
        shop_items: quest.shop_items,
        map_designations: quest.map_designations.val,
    };
}
