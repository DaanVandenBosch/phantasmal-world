import solver from "javascript-lp-solver";
import { ItemType } from "../../core/model/items";
import {
    DifficultyModel,
    DifficultyModels,
    KONDRIEU_PROB,
    RARE_ENEMY_PROB,
    SectionIdModel,
    SectionIdModels,
    ServerModel,
} from "../../core/model";
import { npc_data, NpcType } from "../../core/data_formats/parsing/quest/npc_types";
import { HuntMethodModel } from "../model/HuntMethodModel";
import { Property } from "../../core/observable/property/Property";
import { WritableProperty } from "../../core/observable/property/WritableProperty";
import { OptimalMethodModel, OptimalResultModel, WantedItemModel } from "../model";
import { ListProperty } from "../../core/observable/property/list/ListProperty";
import { list_property, map, property } from "../../core/observable";
import { WritableListProperty } from "../../core/observable/property/list/WritableListProperty";
import { hunt_method_stores } from "./HuntMethodStore";
import { item_drop_stores } from "./ItemDropStore";
import { item_type_stores } from "../../core/stores/ItemTypeStore";
import { hunt_optimizer_persister } from "../persistence/HuntOptimizerPersister";

// TODO: take into account mothmants spawned from mothverts.
// TODO: take into account split slimes.
// TODO: Prefer methods that don't split pan arms over methods that do.
//       For some reason this doesn't actually seem to be a problem, should probably investigate.
// TODO: Show expected value or probability per item per method.
//       Can be useful when deciding which item to hunt first.
// TODO: boxes.
class HuntOptimizerStore {
    readonly huntable_item_types: Property<ItemType[]>;
    // TODO: wanted items per server.
    readonly wanted_items: ListProperty<WantedItemModel>;
    readonly result: Property<OptimalResultModel | undefined>;

    private readonly _wanted_items: WritableListProperty<WantedItemModel> = list_property();
    private readonly _result: WritableProperty<OptimalResultModel | undefined> = property(
        undefined,
    );

    constructor() {
        this.huntable_item_types = map(
            (item_type_store, item_drop_store) => {
                return item_type_store.item_types.filter(
                    item_type =>
                        item_drop_store.enemy_drops.get_drops_for_item_type(item_type.id).length,
                );
            },
            item_type_stores.current.flat_map(current => current),
            item_drop_stores.current.flat_map(current => current),
        );

        this.wanted_items = this._wanted_items;
        this.result = this._result;

        this.initialize_persistence();
    }

    optimize = async () => {
        if (!this.wanted_items.length) {
            this._result.val = undefined;
            return;
        }

        // Initialize this set before awaiting data, so user changes don't affect this optimization
        // run from this point on.
        const wanted_items = new Set(
            this.wanted_items.val.filter(w => w.amount > 0).map(w => w.item_type),
        );

        const methods = await hunt_method_stores.current.val.methods.promise;
        const drop_table = (await item_drop_stores.current.val.promise).enemy_drops;

        // Add a constraint per wanted item.
        const constraints: { [item_name: string]: { min: number } } = {};

        for (const wanted of this.wanted_items.val) {
            constraints[wanted.item_type.name] = { min: wanted.amount };
        }

        // Add a variable to the LP model per method per difficulty per section ID.
        // When a method with pan arms is encountered, two variables are added. One for the method
        // with migiums and hidooms and one with pan arms.
        // Each variable has a time property to minimize and a property per item with the number
        // of enemies that drop the item multiplied by the corresponding drop rate as its value.
        type Variable = {
            time: number;
            [item_name: string]: number;
        };
        const variables: { [method_name: string]: Variable } = {};

        type VariableDetails = {
            method: HuntMethodModel;
            difficulty: DifficultyModel;
            section_id: SectionIdModel;
            split_pan_arms: boolean;
        };
        const variable_details: Map<string, VariableDetails> = new Map();

        for (const method of methods) {
            // Counts include rare enemies, so they are fractional.
            const counts = new Map<NpcType, number>();

            for (const [enemy_type, count] of method.enemy_counts.entries()) {
                const old_count = counts.get(enemy_type) || 0;
                const enemy = npc_data(enemy_type);

                if (enemy.rare_type == null) {
                    counts.set(enemy_type, old_count + count);
                } else {
                    let rate, rare_rate;

                    if (enemy.rare_type === NpcType.Kondrieu) {
                        rate = 1 - KONDRIEU_PROB;
                        rare_rate = KONDRIEU_PROB;
                    } else {
                        rate = 1 - RARE_ENEMY_PROB;
                        rare_rate = RARE_ENEMY_PROB;
                    }

                    counts.set(enemy_type, old_count + count * rate);
                    counts.set(
                        enemy.rare_type,
                        (counts.get(enemy.rare_type) || 0) + count * rare_rate,
                    );
                }
            }

            // Create a secondary counts map if there are any pan arms that can be split into
            // migiums and hidooms.
            const counts_list: Map<NpcType, number>[] = [counts];
            const pan_arms_count = counts.get(NpcType.PanArms);

            if (pan_arms_count) {
                const split_counts = new Map(counts);

                split_counts.delete(NpcType.PanArms);
                split_counts.set(NpcType.Migium, pan_arms_count);
                split_counts.set(NpcType.Hidoom, pan_arms_count);

                counts_list.push(split_counts);
            }

            const pan_arms_2_count = counts.get(NpcType.PanArms2);

            if (pan_arms_2_count) {
                const split_counts = new Map(counts);

                split_counts.delete(NpcType.PanArms2);
                split_counts.set(NpcType.Migium2, pan_arms_2_count);
                split_counts.set(NpcType.Hidoom2, pan_arms_2_count);

                counts_list.push(split_counts);
            }

            for (let i = 0; i < counts_list.length; i++) {
                const counts = counts_list[i];
                const split_pan_arms = i === 1;

                for (const difficulty of DifficultyModels) {
                    for (const section_id of SectionIdModels) {
                        // Will contain an entry per wanted item dropped by enemies in this method/
                        // difficulty/section ID combo.
                        const variable: Variable = {
                            time: method.time.val.as("hours"),
                        };
                        // Only add the variable if the method provides at least 1 item we want.
                        let add_variable = false;

                        for (const [npc_type, count] of counts.entries()) {
                            const drop = drop_table.get_drop(difficulty, section_id, npc_type);

                            if (drop && wanted_items.has(drop.item_type)) {
                                const value = variable[drop.item_type.name] || 0;
                                variable[drop.item_type.name] = value + count * drop.rate;
                                add_variable = true;
                            }
                        }

                        if (add_variable) {
                            const name = this.full_method_name(
                                difficulty,
                                section_id,
                                method,
                                split_pan_arms,
                            );
                            variables[name] = variable;
                            variable_details.set(name, {
                                method,
                                difficulty,
                                section_id,
                                split_pan_arms,
                            });
                        }
                    }
                }
            }
        }

        const result: {
            feasible: boolean;
            bounded: boolean;
            result: number;
            /**
             * Value will always be a number if result is indexed with an actual method name.
             */
            [method: string]: number | boolean;
        } = solver.Solve({
            optimize: "time",
            opType: "min",
            constraints,
            variables,
        });

        if (!result.feasible) {
            this._result.val = undefined;
            return;
        }

        const optimal_methods: OptimalMethodModel[] = [];

        // Loop over the entries in result, ignore standard properties that aren't variables.
        for (const [variable_name, runs_or_other] of Object.entries(result)) {
            const details = variable_details.get(variable_name);

            if (details) {
                const { method, difficulty, section_id, split_pan_arms } = details;
                const runs = runs_or_other as number;
                const variable = variables[variable_name];

                const items = new Map<ItemType, number>();

                for (const [item_name, expected_amount] of Object.entries(variable)) {
                    for (const item of wanted_items) {
                        if (item_name === item.name) {
                            items.set(item, runs * expected_amount);
                            break;
                        }
                    }
                }

                // Find all section IDs that provide the same items with the same expected amount.
                // E.g. if you need a spread needle and a bringer's right arm, using either
                // purplenum or yellowboze will give you the exact same probabilities.
                const section_ids: SectionIdModel[] = [];

                for (const sid of SectionIdModels) {
                    let match_found = true;

                    if (sid !== section_id) {
                        const v =
                            variables[
                                this.full_method_name(difficulty, sid, method, split_pan_arms)
                            ];

                        if (!v) {
                            match_found = false;
                        } else {
                            for (const item_name of Object.keys(variable)) {
                                if (variable[item_name] !== v[item_name]) {
                                    match_found = false;
                                    break;
                                }
                            }
                        }
                    }

                    if (match_found) {
                        section_ids.push(sid);
                    }
                }

                optimal_methods.push(
                    new OptimalMethodModel(
                        difficulty,
                        section_ids,
                        method.name + (split_pan_arms ? " (Split Pan Arms)" : ""),
                        method.episode,
                        method.time.val,
                        runs,
                        items,
                    ),
                );
            }
        }

        this._result.val = new OptimalResultModel([...wanted_items], optimal_methods);
    };

    private full_method_name(
        difficulty: DifficultyModel,
        section_id: SectionIdModel,
        method: HuntMethodModel,
        split_pan_arms: boolean,
    ): string {
        let name = `${difficulty}\t${section_id}\t${method.id}`;
        if (split_pan_arms) name += "\tspa";
        return name;
    }

    private initialize_persistence = async () => {
        this._wanted_items.val = await hunt_optimizer_persister.load_wanted_items(
            ServerModel.Ephinea,
        );

        this._wanted_items.observe_list(() => {
            hunt_optimizer_persister.persist_wanted_items(
                ServerModel.Ephinea,
                this.wanted_items.val,
            );
        });
    };
}

export const hunt_optimizer_store = new HuntOptimizerStore();
