import { Solve } from 'javascript-lp-solver';
import { observable } from "mobx";
import { Difficulties, Item, NpcType, SectionIds } from "../domain";
import { huntMethodStore } from "./HuntMethodStore";
import { itemDropStore } from './ItemDropStore';

export class WantedItem {
    @observable item: Item;
    @observable amount: number;

    constructor(item: Item, amount: number) {
        this.item = item;
        this.amount = amount;
    }
}

class HuntOptimizerStore {
    @observable wantedItems: Array<WantedItem> = [];

    optimize = async () => {
        if (!this.wantedItems.length) return;

        const methods = await huntMethodStore.methods.current.promise;
        const dropTable = await itemDropStore.enemyDrops.current.promise;

        const constraints: { [itemName: string]: { min: number } } = {};

        for (const wanted of this.wantedItems) {
            constraints[wanted.item.name] = { min: wanted.amount };
        }

        const items = new Set(this.wantedItems.map(i => i.item));
        const variables: { [methodName: string]: { [itemName: string]: number } } = {};

        for (const method of methods) {
            const counts = new Map<NpcType, number>();

            for (const enemy of method.quest.enemies) {
                const count = counts.get(enemy.type);
                counts.set(enemy.type, (count || 0) + 1);
            }

            for (const diff of Difficulties) {
                for (const sectionId of SectionIds) {
                    const variable: { [itemName: string]: number } = {
                        time: 0.5
                    };

                    for (const [npcType, count] of counts.entries()) {
                        const drop = dropTable.getDrop(diff, sectionId, npcType);

                        if (drop && items.has(drop.item)) {
                            variable[drop.item.name] = count * drop.rate;
                        }
                    }

                    if (Object.keys(variable).length) {
                        variables[`${diff} ${sectionId} ${method.quest.name}`] = variable;
                    }
                }
            }
        }

        const result = Solve({
            optimize: 'time',
            opType: 'min',
            constraints,
            variables
        });

        console.log(result);
    }
}

export const huntOptimizerStore = new HuntOptimizerStore();

type MethodWithDropRates = {
    name: string
    time: number
    [itemName: string]: any
}
