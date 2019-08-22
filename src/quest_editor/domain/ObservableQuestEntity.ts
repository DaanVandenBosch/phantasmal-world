import { EntityType } from "../../core/data_formats/parsing/quest/entities";

export class ObservableQuestEntity<Type extends EntityType = EntityType> {
    readonly type: Type;

    constructor(type: Type) {
        this.type = type;
    }
}
