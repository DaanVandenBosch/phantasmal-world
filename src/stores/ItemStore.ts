import { observable, IObservableArray } from "mobx";
import { Item } from "../domain";

class ItemStore {
    @observable items: IObservableArray<Item> = observable.array();
}

export const itemStore = new ItemStore();