import { QuestEntityModel } from "./QuestEntityModel";
import { ObjectType } from "../../core/data_formats/parsing/quest/object_types";
import { QuestObject } from "../../core/data_formats/parsing/quest/Quest";
import { defined } from "../../core/util";

export class QuestObjectModel extends QuestEntityModel<ObjectType, QuestObject> {
    constructor(object: QuestObject) {
        defined(object, "object");

        super(object);
    }
}
