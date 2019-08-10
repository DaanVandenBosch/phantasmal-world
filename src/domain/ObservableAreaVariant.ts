import { ObservableArea } from "./ObservableArea";
import { IObservableArray, observable } from "mobx";
import { Section } from "./index";

export class ObservableAreaVariant {
    readonly id: number;
    readonly area: ObservableArea;
    @observable.shallow readonly sections: IObservableArray<Section> = observable.array();

    constructor(id: number, area: ObservableArea) {
        if (!Number.isInteger(id) || id < 0)
            throw new Error(`Expected id to be a non-negative integer, got ${id}.`);

        this.id = id;
        this.area = area;
    }
}
