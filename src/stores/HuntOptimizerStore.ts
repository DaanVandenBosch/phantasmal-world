import solver from 'javascript-lp-solver';
import { autorun, IObservableArray, observable, runInAction } from "mobx";
import { Difficulties, Difficulty, HuntMethod, Item, KONDRIEU_PROB, NpcType, RARE_ENEMY_PROB, SectionId, SectionIds } from "../domain";
import { applicationStore } from './ApplicationStore';
import { huntMethodStore } from "./HuntMethodStore";
import { itemDropStore } from './ItemDropStore';
import { itemStore } from './ItemStore';

export class WantedItem {
    @observable readonly item: Item;
    @observable amount: number;

    constructor(item: Item, amount: number) {
        this.item = item;
        this.amount = amount;
    }
}

export class OptimizationResult {
    public readonly totalTime: number;

    constructor(
        public readonly difficulty: Difficulty,
        public readonly sectionIds: Array<SectionId>,
        public readonly methodName: string,
        public readonly methodTime: number,
        public readonly runs: number,
        public readonly itemCounts: Map<Item, number>
    ) {
        this.totalTime = runs * methodTime;
    }
}

// TODO: Prefer methods that don't split pan arms over methods that do.
//       For some reason this doesn't actually seem to be a problem, should probably investigate.
// TODO: Show expected value or probability per item per method.
//       Can be useful when you want one item "more" than the others.
// TODO: order of items in results table should match order in wanted table.
// TODO: boxes.
class HuntOptimizerStore {
    @observable readonly wantedItems: IObservableArray<WantedItem> = observable.array();
    @observable readonly results: IObservableArray<OptimizationResult> = observable.array();

    constructor() {
        this.initialize();
    }

    initialize = async () => {
        try {
            await this.loadFromLocalStorage();
            autorun(this.storeInLocalStorage);
        } catch (e) {
            console.error(e);
        }
    }

    loadFromLocalStorage = async () => {
        const wantedItemsJson = localStorage.getItem(
            `HuntOptimizerStore.wantedItems.${applicationStore.currentServer}`
        );

        if (wantedItemsJson) {
            const items = await itemStore.items.current.promise;
            const wi = JSON.parse(wantedItemsJson);

            const wantedItems: WantedItem[] = [];

            for (const { itemName, amount } of wi) {
                const item = items.find(item => item.name === itemName);

                if (item) {
                    wantedItems.push(new WantedItem(item, amount));
                }
            }

            this.wantedItems.replace(wantedItems);
        }
    }

    storeInLocalStorage = () => {
        try {
            localStorage.setItem(
                `HuntOptimizerStore.wantedItems.${applicationStore.currentServer}`,
                JSON.stringify(
                    this.wantedItems.map(({ item, amount }) => ({ itemName: item.name, amount }))
                )
            );
        } catch (e) {
            console.error(e);
        }
    }

    optimize = async () => {
        if (!this.wantedItems.length) {
            this.results.splice(0);
            return;
        }

        const methods = await huntMethodStore.methods.current.promise;
        const dropTable = await itemDropStore.enemyDrops.current.promise;

        // Add a constraint per wanted item.
        const constraints: { [itemName: string]: { min: number } } = {};

        for (const wanted of this.wantedItems) {
            constraints[wanted.item.name] = { min: wanted.amount };
        }

        // Add a variable to the LP model per method per difficulty per section ID.
        // When a method with pan arms is encountered, two variables are added. One for the method
        // with migiums and hidooms and one with pan arms.
        // Each variable has a time property to minimize and a property per item with the number
        // of enemies that drop the item multiplied by the corresponding drop rate as its value.
        type Variable = {
            time: number,
            [itemName: string]: number,
        }
        const variables: { [methodName: string]: Variable } = {};

        type VariableDetails = {
            method: HuntMethod,
            difficulty: Difficulty,
            sectionId: SectionId,
            splitPanArms: boolean,
        }
        const variableDetails: Map<string, VariableDetails> = new Map();

        const wantedItems = new Set(this.wantedItems.filter(w => w.amount > 0).map(w => w.item));

        for (const method of methods) {
            // Counts include rare enemies, so they are fractional.
            const counts = new Map<NpcType, number>();

            for (const enemy of method.enemies) {
                const count = counts.get(enemy.type);

                if (enemy.type.rareType == null) {
                    counts.set(enemy.type, (count || 0) + 1);
                } else {
                    let rate, rareRate;

                    if (enemy.type.rareType === NpcType.Kondrieu) {
                        rate = 1 - KONDRIEU_PROB;
                        rareRate = KONDRIEU_PROB;
                    } else {
                        rate = 1 - RARE_ENEMY_PROB;
                        rareRate = RARE_ENEMY_PROB;
                    }

                    counts.set(enemy.type, (count || 0) + rate);

                    const rareCount = counts.get(enemy.type.rareType);
                    counts.set(enemy.type.rareType, (rareCount || 0) + rareRate);
                }
            }

            // Create a secondary counts map if there are any pan arms that can be split into
            // migiums and hidooms.
            const countsList: Array<Map<NpcType, number>> = [counts];
            const panArmsCount = counts.get(NpcType.PanArms);
            const panArms2Count = counts.get(NpcType.PanArms2);

            if (panArmsCount || panArms2Count) {
                const splitCounts = new Map(counts);

                if (panArmsCount) {
                    splitCounts.delete(NpcType.PanArms);
                    splitCounts.set(NpcType.Migium, panArmsCount);
                    splitCounts.set(NpcType.Hidoom, panArmsCount);
                }

                if (panArms2Count) {
                    splitCounts.delete(NpcType.PanArms2);
                    splitCounts.set(NpcType.Migium2, panArms2Count);
                    splitCounts.set(NpcType.Hidoom2, panArms2Count);
                }

                countsList.push(splitCounts);
            }

            for (let i = 0; i < countsList.length; i++) {
                const counts = countsList[i];
                const splitPanArms = i === 1;

                for (const diff of Difficulties) {
                    for (const sectionId of SectionIds) {
                        const variable: Variable = {
                            time: method.time
                        };
                        let addVariable = false;

                        for (const [npcType, count] of counts.entries()) {
                            const drop = dropTable.getDrop(diff, sectionId, npcType);

                            if (drop && wantedItems.has(drop.item)) {
                                const value = variable[drop.item.name] || 0;
                                variable[drop.item.name] = value + count * drop.rate;
                                addVariable = true;
                            }
                        }

                        if (addVariable) {
                            const name = this.fullMethodName(
                                diff, sectionId, method, splitPanArms
                            );
                            variables[name] = variable;
                            variableDetails.set(name, {
                                method,
                                difficulty: diff,
                                sectionId,
                                splitPanArms
                            });
                        }
                    }
                }
            }
        }

        const result: {
            feasible: boolean,
            bounded: boolean,
            result: number,
            [method: string]: number | boolean
        } = solver.Solve({
            optimize: 'time',
            opType: 'min',
            constraints,
            variables
        });

        runInAction(() => {
            this.results.splice(0);

            if (!result.feasible) {
                return;
            }

            for (const [variableName, runsOrOther] of Object.entries(result)) {
                const details = variableDetails.get(variableName);

                if (details) {
                    const { method, difficulty, sectionId, splitPanArms } = details;
                    const runs = runsOrOther as number;
                    const variable = variables[variableName];

                    const items = new Map<Item, number>();

                    for (const [itemName, expectedAmount] of Object.entries(variable)) {
                        for (const item of wantedItems) {
                            if (itemName === item.name) {
                                items.set(item, runs * expectedAmount);
                                break;
                            }
                        }
                    }

                    // Find all section IDs that provide the same items with the same expected amount.
                    // E.g. if you need a spread needle and a bringer's right arm, using either
                    // purplenum or yellowboze will give you the exact same probabilities.
                    const sectionIds: Array<SectionId> = [];

                    for (const sid of SectionIds) {
                        let matchFound = true;

                        if (sid !== sectionId) {
                            const v = variables[
                                this.fullMethodName(difficulty, sid, method, splitPanArms)
                            ];

                            if (!v) {
                                matchFound = false;
                            } else {
                                for (const itemName of Object.keys(variable)) {
                                    if (variable[itemName] !== v[itemName]) {
                                        matchFound = false;
                                        break;
                                    }
                                }
                            }
                        }

                        if (matchFound) {
                            sectionIds.push(sid);
                        }
                    }

                    this.results.push(new OptimizationResult(
                        difficulty,
                        sectionIds,
                        method.name + (splitPanArms ? ' (Split Pan Arms)' : ''),
                        method.time,
                        runs,
                        items
                    ));
                }
            }
        });
    }

    private fullMethodName(
        difficulty: Difficulty,
        sectionId: SectionId,
        method: HuntMethod,
        splitPanArms: boolean
    ): string {
        let name = `${difficulty}\t${sectionId}\t${method.name}`;
        if (splitPanArms) name += ' (Split Pan Arms)';
        return name;
    }
}

export const huntOptimizerStore = new HuntOptimizerStore();
