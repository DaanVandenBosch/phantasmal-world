import { EntityListView } from "./EntityListView";
import {
    object_data,
    OBJECT_TYPES,
    ObjectType,
} from "../../core/data_formats/parsing/quest/object_types";
import { quest_editor_store } from "../stores/QuestEditorStore";
import { Episode } from "../../core/data_formats/parsing/quest/Episode";

export class ObjectListView extends EntityListView<ObjectType> {
    constructor() {
        super("quest_editor_ObjectListView");

        this.disposables(
            quest_editor_store.current_quest.observe(this.filter_objects),
            quest_editor_store.current_area.observe(this.filter_objects),
        );

        this.filter_objects();
        this.finalize_construction();
    }

    private filter_objects = (): void => {
        const quest = quest_editor_store.current_quest.val;
        const area = quest_editor_store.current_area.val;

        const episode = quest ? quest.episode : Episode.I;
        const area_id = area ? area.id : 0;

        this.entities.val = OBJECT_TYPES.filter(object => {
            const data = object_data(object);

            if (data.area_ids == undefined) return true;

            const area_ids = data.area_ids[episode];
            return area_ids && area_ids.includes(area_id);
        });
    };
}
