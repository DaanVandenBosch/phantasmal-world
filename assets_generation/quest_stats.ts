import { walk_quests } from "./walk_quests";
import { RESOURCE_DIR } from "./index";
import { NpcType } from "../src/core/data_formats/parsing/quest/npc_types";
import { QuestNpc } from "../src/core/data_formats/parsing/quest/QuestNpc";
import { EntityProp, EntityPropType } from "../src/core/data_formats/parsing/quest/properties";
import {
    entity_data,
    EntityType,
    get_entity_prop_value,
    get_entity_type,
    is_npc_type,
    Quest,
    QuestEntity,
} from "../src/core/data_formats/parsing/quest/Quest";
import { ObjectType } from "../src/core/data_formats/parsing/quest/object_types";
import { QuestObject } from "../src/core/data_formats/parsing/quest/QuestObject";

const prop_cache = new Map<EntityType, EntityProp[]>();

print_quest_entity_stats({ npcs: true, objects: true, print: "stats" });

function print_quest_entity_stats(config: {
    npcs?: boolean;
    objects?: boolean;
    print?: "stats" | "code";
}): void {
    const npcs_by_type: Map<
        NpcType,
        { entity: QuestNpc; quest: string; count: number }[]
    > = new Map();
    const objects_by_type: Map<
        ObjectType,
        { entity: QuestObject; quest: string; count: number }[]
    > = new Map();

    walk_quests(
        {
            path: `${RESOURCE_DIR}/tethealla_v0.143_quests`,
            exclude: ["/battle", "/chl/ep1", "/chl/ep4", "/shop"],
        },
        ({ quest }) => {
            if (config.npcs) {
                process_entities(quest, quest.npcs, npcs_by_type);
            }

            if (config.objects) {
                process_entities(quest, quest.objects, objects_by_type);
            }
        },
    );

    if (config.npcs) {
        if (config.print === "code") {
            print_entity_code(npcs_by_type);
        } else {
            print_entity_property_stats(npcs_by_type);
        }
    }

    if (config.objects) {
        if (config.print === "code") {
            print_entity_code(objects_by_type);
        } else {
            print_entity_property_stats(objects_by_type);
        }
    }
}

function print_entity_property_stats(
    entities_by_type: Map<EntityType, { entity: QuestEntity; quest: string; count: number }[]>,
): void {
    const sorted = [...entities_by_type.entries()].sort(([a_type], [b_type]) => a_type - b_type);

    for (const [type, entities] of sorted) {
        const props = get_properties(type);

        /* eslint-disable no-console */
        console.log(ObjectType[type] ?? NpcType[type]);
        console.log("    cnt " + props.map(col_print_name).join(" "));

        const sorted = entities.sort((a, b) => b.count - a.count).slice(0, 5);

        for (const { entity, quest, count } of sorted) {
            console.log(
                `    ${count.toString().padStart(3, " ")} ` +
                    props.map(p => col_print_value(entity, p)).join(" ") +
                    ` ${quest}`,
            );
        }
        /* eslint-enable no-console */
    }
}

/**
 * Used to populate the switch in set_npc_default_data or set_object_default_data.
 * Prints code of the following form (view is assumed to be a DataView).
 *
 * ```ts
 * case EntityType.$ENTITY_TYPE:
 *     view.set$PROP_TYPE($OFFSET, $VALUE, true); // $PROP_NAME
 *     break;
 * ```
 */
function print_entity_code(
    entities_by_type: Map<EntityType, { entity: QuestEntity; quest: string; count: number }[]>,
): void {
    for (const [type, entities] of [...entities_by_type.entries()].sort(
        ([a_type], [b_type]) => a_type - b_type,
    )) {
        const is_npc = is_npc_type(type);
        const { entity } = entities.sort((a, b) => b.count - a.count)[0];

        const props = get_properties(type)
            .map(prop => {
                const value =
                    prop.type === EntityPropType.Angle
                        ? entity.view.getInt32(prop.offset, true)
                        : get_entity_prop_value(entity, prop);
                return [prop, value] as const;
            })
            .filter(([prop, value]) => {
                if (!is_npc && prop.offset >= 40 && prop.offset < 52) {
                    return value !== 1;
                } else {
                    return value !== 0;
                }
            });

        if (props.length === 0) continue;

        /* eslint-disable no-console */
        if (is_npc) {
            console.log(`case NpcType.${NpcType[type]}:`);
        } else {
            console.log(`case ObjectType.${ObjectType[type]}:`);
        }

        for (const [prop, value] of props) {
            let prop_type: string;

            switch (prop.type) {
                case EntityPropType.U8:
                    prop_type = "Uint8";
                    break;
                case EntityPropType.U16:
                    prop_type = "Uint16";
                    break;
                case EntityPropType.U32:
                    prop_type = "Uint32";
                    break;
                case EntityPropType.I8:
                    prop_type = "Int8";
                    break;
                case EntityPropType.I16:
                    prop_type = "Int16";
                    break;
                case EntityPropType.I32:
                    prop_type = "Int32";
                    break;
                case EntityPropType.F32:
                    prop_type = "Float32";
                    break;
                case EntityPropType.Angle:
                    prop_type = "Int32";
                    break;
                default:
                    throw new Error(`EntityPropType.${EntityPropType[prop.type]} not supported.`);
            }

            const offset = prop.offset;
            const comment = prop.name === "Unknown" ? "" : ` // ${prop.name}`;

            console.log(`    view.set${prop_type}(${offset}, ${value}, true);${comment}`);
        }

        console.log("    break;");
        /* eslint-enable no-console */
    }
}

function process_entities(
    quest: Quest,
    entities: readonly QuestEntity[],
    entities_by_type: Map<EntityType, { entity: QuestEntity; quest: string; count: number }[]>,
): void {
    for (const entity of entities) {
        const type = get_entity_type(entity);
        const existing_entities = entities_by_type.get(type);

        if (existing_entities == undefined) {
            entities_by_type.set(type, [{ entity, quest: quest.name, count: 1 }]);
        } else {
            const found = existing_entities.find(({ entity: entity_2 }) =>
                entities_equal(entity, entity_2, type),
            );

            if (found) {
                found.count++;
            } else {
                existing_entities.push({ entity, quest: quest.name, count: 1 });
            }
        }
    }
}

/**
 * @returns the entity's properties enriched with many default properties.
 */
function get_properties(type: EntityType): EntityProp[] {
    let props = prop_cache.get(type);

    if (props) {
        return props;
    }

    if (is_npc_type(type)) {
        props = [
            {
                name: "Unknown",
                offset: 2,
                type: EntityPropType.I16,
            },
            {
                name: "Unknown",
                offset: 4,
                type: EntityPropType.I16,
            },
            {
                name: "Clone count",
                offset: 6,
                type: EntityPropType.I16,
            },
            {
                name: "Unknown",
                offset: 8,
                type: EntityPropType.I16,
            },
            {
                name: "Unknown",
                offset: 10,
                type: EntityPropType.I16,
            },
            {
                name: "Scale x",
                offset: 44,
                type: EntityPropType.F32,
            },
            {
                name: "Scale y",
                offset: 48,
                type: EntityPropType.F32,
            },
            {
                name: "Scale z",
                offset: 52,
                type: EntityPropType.F32,
            },
            {
                name: "Unknown",
                offset: 68,
                type: EntityPropType.I16,
            },
            {
                name: "Unknown",
                offset: 70,
                type: EntityPropType.I16,
            },
        ];
    } else {
        props = [
            {
                name: "Unknown",
                offset: 2,
                type: EntityPropType.I16,
            },
            {
                name: "Unknown",
                offset: 4,
                type: EntityPropType.I16,
            },
            {
                name: "Unknown",
                offset: 6,
                type: EntityPropType.I16,
            },
            {
                name: "Unknown",
                offset: 14,
                type: EntityPropType.I16,
            },
            {
                name: "Scale x",
                offset: 40,
                type: EntityPropType.F32,
            },
            {
                name: "Scale y",
                offset: 44,
                type: EntityPropType.F32,
            },
            {
                name: "Scale z",
                offset: 48,
                type: EntityPropType.F32,
            },
            {
                name: "Unknown",
                offset: 56,
                type: EntityPropType.I32,
            },
            {
                name: "Unknown",
                offset: 60,
                type: EntityPropType.I32,
            },
            {
                name: "Unknown",
                offset: 64,
                type: EntityPropType.I32,
            },
        ];
    }

    outer: for (const entity_prop of entity_data(type).properties) {
        for (let i = 0; i < props.length; i++) {
            const prop = props[i];

            if (entity_prop.offset === prop.offset) {
                props.splice(i, 1, entity_prop);
                continue outer;
            } else if (entity_prop.offset < prop.offset) {
                props.splice(i, 0, entity_prop);
                continue outer;
            }
        }

        props.push(entity_prop);
    }

    return props;
}

function col_print_name(prop: EntityProp): string {
    const width = col_width(prop);
    return prop.name.slice(0, width).padStart(width, " ");
}

function col_print_value(entity: QuestEntity, prop: EntityProp): string {
    const value = get_entity_prop_value(entity, prop);
    const str =
        prop.type === EntityPropType.F32 || prop.type === EntityPropType.Angle
            ? value.toFixed(3)
            : value.toString();
    return str.padStart(col_width(prop), " ");
}

function col_width(prop: EntityProp): number {
    switch (prop.type) {
        case EntityPropType.U8:
            return 3;
        case EntityPropType.U16:
            return 5;
        case EntityPropType.U32:
            return 10;
        case EntityPropType.I8:
            return 4;
        case EntityPropType.I16:
            return 6;
        case EntityPropType.I32:
            return 11;
        case EntityPropType.F32:
            return 10;
        case EntityPropType.Angle:
            return 4;
        default:
            throw new Error(`EntityPropType.${EntityPropType[prop.type]} not supported.`);
    }
}

function entities_equal(a: QuestEntity, b: QuestEntity, type: EntityType): boolean {
    for (const prop of get_properties(type)) {
        if (get_entity_prop_value(a, prop) !== get_entity_prop_value(b, prop)) {
            return false;
        }
    }

    return true;
}
