import { QuestRenderer } from "./QuestRenderer";
import { quest_editor_store } from "../stores/QuestEditorStore";
import { AreaVariantDetails, QuestModelManager } from "./QuestModelManager";
import { AreaVariantModel } from "../model/AreaVariantModel";
import { QuestNpcModel } from "../model/QuestNpcModel";
import { QuestObjectModel } from "../model/QuestObjectModel";
import { ListProperty } from "../../core/observable/property/list/ListProperty";
import { list_property } from "../../core/observable";

/**
 * Model loader used while editing a quest.
 */
export class QuestEditorModelManager extends QuestModelManager {
    constructor(renderer: QuestRenderer) {
        super(renderer);

        this.disposer.add_all(
            quest_editor_store.current_quest.observe(this.area_variant_changed),
            quest_editor_store.current_area.observe(this.area_variant_changed),
        );
    }

    protected get_area_variant_details(): AreaVariantDetails {
        console.log("get_area_variant_details")
        const quest = quest_editor_store.current_quest.val;
        const area = quest_editor_store.current_area.val;

        let area_variant: AreaVariantModel | undefined;
        let npcs: ListProperty<QuestNpcModel>;
        let objects: ListProperty<QuestObjectModel>;

        if (quest && area) {
            area_variant =
                quest.area_variants.val.find(v => v.area.id === area.id) || area.area_variants[0];

            npcs = quest.npcs.filtered(entity => entity.area_id === area.id);
            objects = quest.objects.filtered(entity => entity.area_id === area.id);
        } else {
            npcs = list_property();
            objects = list_property();
        }

        return {
            episode: quest?.episode,
            area_variant,
            npcs,
            objects,
        };
    }
}
