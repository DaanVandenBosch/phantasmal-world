import { EntityListView } from "./EntityListView";
import { OBJECT_TYPES, ObjectType } from "../../core/data_formats/parsing/quest/object_types";

export class ObjectListView extends EntityListView<ObjectType> {
    constructor() {
        super("quest_editor_ObjectListView");

        this.entities.val = OBJECT_TYPES;

        this.finalize_construction(ObjectListView.prototype);
    }
}
