import { observable, IObservableArray } from "mobx";
import { HuntMethod } from "../domain";

class HuntMethodStore {
    @observable methods: IObservableArray<HuntMethod> = observable.array();
}

export const huntMethodStore = new HuntMethodStore();
