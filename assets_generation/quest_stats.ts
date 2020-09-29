import { walk_quests } from "./walk_quests";
import { RESOURCE_DIR } from "./index";
import { npc_data, NpcType } from "../src/core/data_formats/parsing/quest/npc_types";
import { get_npc_type, QuestNpc } from "../src/core/data_formats/parsing/quest/QuestNpc";
import { EntityProp, EntityPropType } from "../src/core/data_formats/parsing/quest/properties";
import { get_entity_prop_value, QuestEntity } from "../src/core/data_formats/parsing/quest/Quest";

const prop_cache = new Map<NpcType, EntityProp[]>();

print_quest_stats();

function print_quest_stats(): void {
    const type_data: Map<NpcType, { npc: QuestNpc; quest: string; count: number }[]> = new Map();

    walk_quests(
        {
            path: `${RESOURCE_DIR}/tethealla_v0.143_quests`,
            exclude: ["/battle", "/chl/ep1", "/chl/ep4", "/shop"],
        },
        ({ quest }) => {
            for (const npc of quest.npcs) {
                const type = get_npc_type(npc);
                const npcs = type_data.get(type);

                if (npcs == undefined) {
                    type_data.set(type, [{ npc, quest: quest.name, count: 1 }]);
                } else {
                    const found = npcs.find(({ npc: npc_2 }) => entities_equal(npc, npc_2, type));

                    if (found) {
                        found.count++;
                    } else {
                        npcs.push({ npc, quest: quest.name, count: 1 });
                    }
                }
            }
        },
    );

    for (const [type, npcs] of [...type_data.entries()].sort(
        ([a_type], [b_type]) => a_type - b_type,
    )) {
        const props = get_properties(type);

        /* eslint-disable no-console */
        console.log(NpcType[type]);
        console.log("    cnt " + props.map(col_print_name).join(" "));

        for (const { npc, quest, count } of npcs.sort((a, b) => b.count - a.count).slice(0, 5)) {
            console.log(
                `    ${count.toString().padStart(3, " ")} ` +
                    props.map(p => col_print_value(npc, p)).join(" ") +
                    ` ${quest}`,
            );
        }
        /* eslint-enable no-console */
    }

    // Used to populate the switch in set_npc_default_data.
    // Prints code of the following form (view is assumed to be a DataView).
    //
    // case NpcType.$NPC_TYPE:
    //     view.set$PROP_TYPE($OFFSET, $VALUE, true); // $PROP_NAME
    //     break;
    //
    // for (const [type, npcs] of [...type_data.entries()].sort(
    //     ([a_type], [b_type]) => a_type - b_type,
    // )) {
    //     const { npc } = npcs.sort((a, b) => b.count - a.count)[0];
    //
    //     const props = get_properties(type)
    //         .map(prop => {
    //             const value =
    //                 prop.type === EntityPropType.Angle
    //                     ? npc.view.getInt32(prop.offset, true)
    //                     : get_entity_prop_value(npc, prop);
    //             return [prop, value] as const;
    //         })
    //         .filter(([, value]) => value !== 0);
    //
    //     if (props.length === 0) continue;
    //
    //     /* eslint-disable no-console */
    //     console.log(`case NpcType.${NpcType[type]}:`);
    //
    //     for (const [prop, value] of props) {
    //         let prop_type: string;
    //
    //         switch (prop.type) {
    //             case EntityPropType.U8:
    //                 prop_type = "Uint8";
    //                 break;
    //             case EntityPropType.U16:
    //                 prop_type = "Uint16";
    //                 break;
    //             case EntityPropType.U32:
    //                 prop_type = "Uint32";
    //                 break;
    //             case EntityPropType.I8:
    //                 prop_type = "Int8";
    //                 break;
    //             case EntityPropType.I16:
    //                 prop_type = "Int16";
    //                 break;
    //             case EntityPropType.I32:
    //                 prop_type = "Int32";
    //                 break;
    //             case EntityPropType.F32:
    //                 prop_type = "Float32";
    //                 break;
    //             case EntityPropType.Angle:
    //                 prop_type = "Int32";
    //                 break;
    //             default:
    //                 throw new Error(`EntityPropType.${EntityPropType[prop.type]} not supported.`);
    //         }
    //
    //         const offset = prop.offset;
    //         const comment = prop.name === "Unknown" ? "" : ` // ${prop.name}`;
    //
    //         console.log(`    view.set${prop_type}(${offset}, ${value}, true);${comment}`);
    //     }
    //
    //     console.log("    break;");
    //     /* eslint-enable no-console */
    // }
}

/**
 * @returns the entity's properties enriched with many default properties.
 */
function get_properties(type: NpcType): EntityProp[] {
    let props = prop_cache.get(type);

    if (props) {
        return props;
    }

    const data = npc_data(type);

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

    outer: for (const npc_prop of data.properties) {
        for (let i = 0; i < props.length; i++) {
            const prop = props[i];

            if (npc_prop.offset === prop.offset) {
                props.splice(i, 1, npc_prop);
                continue outer;
            } else if (npc_prop.offset < prop.offset) {
                props.splice(i, 0, npc_prop);
                continue outer;
            }
        }

        props.push(npc_prop);
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

function entities_equal(a: QuestEntity, b: QuestEntity, type: NpcType): boolean {
    for (const prop of get_properties(type)) {
        if (get_entity_prop_value(a, prop) !== get_entity_prop_value(b, prop)) {
            return false;
        }
    }

    return true;
}
