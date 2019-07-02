import solver from "javascript-lp-solver";
import { autorun, IObservableArray, observable, computed } from "mobx";
import {
    Difficulties,
    Difficulty,
    HuntMethod,
    ItemType,
    KONDRIEU_PROB,
    NpcType,
    RARE_ENEMY_PROB,
    SectionId,
    SectionIds,
    Server,
    Episode,
} from "../domain";
import { application_store } from "./ApplicationStore";
import { hunt_method_store } from "./HuntMethodStore";
import { item_drop_stores } from "./ItemDropStore";
import { item_type_stores } from "./ItemTypeStore";
import Logger from "js-logger";

const logger = Logger.get("stores/HuntOptimizerStore");

export class WantedItem {
    @observable readonly item_type: ItemType;
    @observable amount: number;

    constructor(item_type: ItemType, amount: number) {
        this.item_type = item_type;
        this.amount = amount;
    }
}

export class OptimalResult {
    readonly wanted_items: ItemType[];
    readonly optimal_methods: OptimalMethod[];

    constructor(wanted_items: ItemType[], optimal_methods: OptimalMethod[]) {
        this.wanted_items = wanted_items;
        this.optimal_methods = optimal_methods;
    }
}

export class OptimalMethod {
    readonly difficulty: Difficulty;
    readonly section_ids: SectionId[];
    readonly method_name: string;
    readonly method_episode: Episode;
    readonly method_time: number;
    readonly runs: number;
    readonly total_time: number;
    readonly item_counts: Map<ItemType, number>;

    constructor(
        difficulty: Difficulty,
        section_ids: SectionId[],
        method_name: string,
        method_episode: Episode,
        method_time: number,
        runs: number,
        item_counts: Map<ItemType, number>
    ) {
        this.difficulty = difficulty;
        this.section_ids = section_ids;
        this.method_name = method_name;
        this.method_episode = method_episode;
        this.method_time = method_time;
        this.runs = runs;
        this.total_time = runs * method_time;
        this.item_counts = item_counts;
    }
}

// TODO: take into account mothmants spawned from mothverts.
// TODO: take into account split slimes.
// TODO: Prefer methods that don't split pan arms over methods that do.
//       For some reason this doesn't actually seem to be a problem, should probably investigate.
// TODO: Show expected value or probability per item per method.
//       Can be useful when deciding which item to hunt first.
// TODO: boxes.
class HuntOptimizerStore {
    @computed get huntable_item_types(): ItemType[] {
        const item_drop_store = item_drop_stores.current.value;
        return item_type_stores.current.value.item_types.filter(
            i => item_drop_store.enemy_drops.get_drops_for_item_type(i.id).length
        );
    }

    // TODO: wanted items per server.
    @observable readonly wanted_items: IObservableArray<WantedItem> = observable.array();
    @observable result?: OptimalResult;

    constructor() {
        this.initialize();
    }

    optimize = async () => {
        if (!this.wanted_items.length) {
            this.result = undefined;
            return;
        }

        // Initialize this set before awaiting data, so user changes don't affect this optimization
        // run from this point on.
        const wanted_items = new Set(
            this.wanted_items.filter(w => w.amount > 0).map(w => w.item_type)
        );

        const methods = await hunt_method_store.methods.current.promise;
        const drop_table = (await item_drop_stores.current.promise).enemy_drops;

        // Add a constraint per wanted item.
        const constraints: { [item_name: string]: { min: number } } = {};

        for (const wanted of this.wanted_items) {
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
            method: HuntMethod;
            difficulty: Difficulty;
            section_id: SectionId;
            split_pan_arms: boolean;
        };
        const variable_details: Map<string, VariableDetails> = new Map();

        for (const method of methods) {
            // Counts include rare enemies, so they are fractional.
            const counts = new Map<NpcType, number>();

            for (const [enemy, count] of method.enemy_counts.entries()) {
                const old_count = counts.get(enemy) || 0;

                if (enemy.rare_type == null) {
                    counts.set(enemy, old_count + count);
                } else {
                    let rate, rare_rate;

                    if (enemy.rare_type === NpcType.Kondrieu) {
                        rate = 1 - KONDRIEU_PROB;
                        rare_rate = KONDRIEU_PROB;
                    } else {
                        rate = 1 - RARE_ENEMY_PROB;
                        rare_rate = RARE_ENEMY_PROB;
                    }

                    counts.set(enemy, old_count + count * rate);
                    counts.set(
                        enemy.rare_type,
                        (counts.get(enemy.rare_type) || 0) + count * rare_rate
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

                for (const difficulty of Difficulties) {
                    for (const section_id of SectionIds) {
                        // Will contain an entry per wanted item dropped by enemies in this method/
                        // difficulty/section ID combo.
                        const variable: Variable = {
                            time: method.time,
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
                                split_pan_arms
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
            this.result = undefined;
            return;
        }

        const optimal_methods: OptimalMethod[] = [];

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
                const section_ids: SectionId[] = [];

                for (const sid of SectionIds) {
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
                    new OptimalMethod(
                        difficulty,
                        section_ids,
                        method.name + (split_pan_arms ? " (Split Pan Arms)" : ""),
                        method.episode,
                        method.time,
                        runs,
                        items
                    )
                );
            }
        }

        this.result = new OptimalResult([...wanted_items], optimal_methods);
    };

    private full_method_name(
        difficulty: Difficulty,
        section_id: SectionId,
        method: HuntMethod,
        split_pan_arms: boolean
    ): string {
        let name = `${difficulty}\t${section_id}\t${method.id}`;
        if (split_pan_arms) name += "\tspa";
        return name;
    }

    private initialize = async () => {
        try {
            await this.load_from_local_storage();
            autorun(this.store_in_local_storage);
        } catch (e) {
            logger.error(e);
        }
    };

    private load_from_local_storage = async () => {
        const wanted_items_json = localStorage.getItem(
            `HuntOptimizerStore.wantedItems.${Server[application_store.current_server]}`
        );

        if (wanted_items_json) {
            const item_store = await item_type_stores.current.promise;
            const wi: StoredWantedItem[] = JSON.parse(wanted_items_json);

            const wanted_items: WantedItem[] = [];

            for (const { itemTypeId, itemKindId, amount } of wi) {
                const item =
                    itemTypeId != undefined
                        ? item_store.get_by_id(itemTypeId)
                        : item_store.get_by_id(itemKindId!);

                if (item) {
                    wanted_items.push(new WantedItem(item, amount));
                }
            }

            this.wanted_items.replace(wanted_items);
        }
    };

    private store_in_local_storage = () => {
        try {
            localStorage.setItem(
                `HuntOptimizerStore.wantedItems.${Server[application_store.current_server]}`,
                JSON.stringify(
                    this.wanted_items.map(
                        ({ item_type: itemType, amount }): StoredWantedItem => ({
                            itemTypeId: itemType.id,
                            amount,
                        })
                    )
                )
            );
        } catch (e) {
            logger.error(e);
        }
    };
}

type StoredWantedItem = {
    itemTypeId?: number; // Should only be undefined if the legacy name is still used.
    itemKindId?: number; // Legacy name.
    amount: number;
};

export const hunt_optimizer_store = new HuntOptimizerStore();
