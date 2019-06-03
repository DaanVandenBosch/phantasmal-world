import { Solve } from 'javascript-lp-solver';
import { observable } from "mobx";
import { Item, SectionIds } from "../domain";
import { huntMethodStore } from "./HuntMethodStore";

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

        const constraints: { [itemName: string]: { min: number } } = {};

        for (const item of this.wantedItems) {
            constraints[item.item.name] = { min: item.amount };
        }

        const variables: { [methodName: string]: { [itemName: string]: number } } = {};

        for (const method of await huntMethodStore.methods.current.promise) {
            for (const sectionId of SectionIds) {
                const variable = {};

                // TODO

                variables[`${sectionId} ${method.quest.name}`] = variable;
            }
        }

        const result = Solve({
            optimize: '',
            opType: 'min',
            constraints,
            variables
        });
    }
}

export const huntOptimizerStore = new HuntOptimizerStore();

type MethodWithDropRates = {
    name: string
    time: number
    [itemName: string]: any
}
