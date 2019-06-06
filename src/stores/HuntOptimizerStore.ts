import solver from 'javascript-lp-solver';
import { IObservableArray, observable, runInAction } from "mobx";
import { Difficulties, Difficulty, Item, NpcType, SectionId, SectionIds, KONDRIEU_PROB, RARE_ENEMY_PROB, HuntMethod } from "../domain";
import { huntMethodStore } from "./HuntMethodStore";
import { itemDropStore } from './ItemDropStore';

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
        public readonly sectionId: SectionId,
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
// TODO: save state in url for easy sharing (supporting custom methods will be hard though).
// TODO: group similar methods (e.g. same difficulty, same quest and similar ID).
//       This way people can choose their preferred section ID.
// TODO: order of items in results table should match order in wanted table.
// TODO: boxes.
class HuntOptimizerStore {
    @observable readonly wantedItems: Array<WantedItem> = [];
    @observable readonly results: IObservableArray<OptimizationResult> = observable.array();

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

            for (const enemy of method.quest.enemies) {
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
                            let name = `${diff}\t${sectionId}\t${method.name}`;

                            if (splitPanArms) {
                                name += ' (Split Pan Arms)';
                            }

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

                    for (const [itemName, expectedValue] of Object.entries(variable)) {
                        for (const item of wantedItems) {
                            if (itemName === item.name) {
                                items.set(item, runs * expectedValue);
                                break;
                            }
                        }
                    }

                    this.results.push(new OptimizationResult(
                        difficulty,
                        sectionId,
                        method.name + (splitPanArms ? ' (Split Pan Arms)' : ''),
                        method.time,
                        runs,
                        items
                    ));
                }
            }
        });
    }
}

export const huntOptimizerStore = new HuntOptimizerStore();
