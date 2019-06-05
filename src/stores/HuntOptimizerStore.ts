import solver from 'javascript-lp-solver';
import { IObservableArray, observable, runInAction } from "mobx";
import { Difficulties, Difficulty, Item, NpcType, SectionId, SectionIds } from "../domain";
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

// TODO: group similar methods (e.g. same difficulty, same quest and similar ID).
// This way people can choose their preferred section ID.
// TODO: Cutter doesn't seem to work.
class HuntOptimizerStore {
    @observable readonly wantedItems: Array<WantedItem> = [];
    @observable readonly result: IObservableArray<OptimizationResult> = observable.array();

    optimize = async () => {
        if (!this.wantedItems.length) return;

        const methods = await huntMethodStore.methods.current.promise;
        const dropTable = await itemDropStore.enemyDrops.current.promise;

        // Add a constraint per wanted item.
        const constraints: { [itemName: string]: { min: number } } = {};

        for (const wanted of this.wantedItems) {
            constraints[wanted.item.name] = { min: wanted.amount };
        }

        // Add a variable to the LP model per method per difficulty per section ID.
        // Each variable has a time property to minimize and a property per item with the number
        // of enemies that drop the item multiplied by the corresponding drop rate as its value.
        type Variable = {
            time: number,
            [itemName: string]: number
        }
        const variables: { [methodName: string]: Variable } = {};

        const wantedItems = new Set(this.wantedItems.map(i => i.item));

        for (const method of methods) {
            const counts = new Map<NpcType, number>();

            for (const enemy of method.quest.enemies) {
                const count = counts.get(enemy.type);
                counts.set(enemy.type, (count || 0) + 1);
            }

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
                        variables[`${diff}\t${sectionId}\t${method.quest.name}`] = variable;
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
            this.result.splice(0);

            for (const [method, runsOrOther] of Object.entries(result)) {
                const [diffStr, sIdStr, methodName] = method.split('\t', 3);

                if (sIdStr && methodName) {
                    const runs = runsOrOther as number;
                    const variable = variables[method];
                    const diff = (Difficulty as any)[diffStr];
                    const sectionId = (SectionId as any)[sIdStr];

                    const items = new Map<Item, number>();

                    for (const [itemName, expectedValue] of Object.entries(variable)) {
                        for (const item of wantedItems) {
                            if (itemName === item.name) {
                                items.set(item, runs * expectedValue);
                                break;
                            }
                        }
                    }

                    this.result.push(new OptimizationResult(
                        diff,
                        sectionId,
                        methodName,
                        0.5,
                        runs,
                        items
                    ));
                }
            }
        });
    }
}

export const huntOptimizerStore = new HuntOptimizerStore();
