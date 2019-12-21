import { QuestRenderer } from "./QuestRenderer";
import { AreaVariantDetails, QuestModelManager } from "./QuestModelManager";
import { QuestRunner } from "../QuestRunner";

/**
 * Model loader used while running a quest.
 */
export class QuestRunnerModelManager extends QuestModelManager {
    constructor(private readonly quest_runner: QuestRunner, renderer: QuestRenderer) {
        super(renderer);

        this.disposer.add_all(
            this.quest_runner.game_state.current_area_variant.observe(this.area_variant_changed, {
                call_now: true,
            }),
        );
    }

    protected get_area_variant_details(): AreaVariantDetails {
        const game_state = this.quest_runner.game_state;

        return {
            episode: game_state.episode,
            area_variant: game_state.current_area_variant.val,
            npcs: game_state.npcs,
            objects: game_state.objects,
        };
    }
}
