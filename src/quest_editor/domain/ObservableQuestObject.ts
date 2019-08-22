import { ObservableQuestEntity } from "./ObservableQuestEntity";
import { ObjectType } from "../../core/data_formats/parsing/quest/object_types";

export class ObservableQuestObject extends ObservableQuestEntity<ObjectType> {
    constructor(type: ObjectType) {
        super(type);
    }
}
