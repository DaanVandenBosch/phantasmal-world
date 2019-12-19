import { QuestRenderer } from "./QuestRenderer";
import { quest_editor_store } from "../stores/QuestEditorStore";
import { AreaVariantDetails, QuestModelManager } from "./QuestModelManager";

/**
 * Model loader used while running a quest.
 */
export class QuestRunnerModelManager extends QuestModelManager {
    constructor(renderer: QuestRenderer) {
        super(renderer);

        this.disposer.add_all(
            quest_editor_store.quest_runner.game_state.current_area_variant.observe(
                this.area_variant_changed,
                { call_now: true },
            ),
        );
    }

    protected get_area_variant_details(): AreaVariantDetails {
        const game_state = quest_editor_store.quest_runner.game_state;

        return {
            episode: game_state.episode,
            area_variant: game_state.current_area_variant.val,
            npcs: game_state.npcs,
            objects: game_state.objects,
        };
    }
}
