import { QuestRenderer } from "./QuestRenderer";
import { AreaVariantDetails, Quest3DModelManager } from "./Quest3DModelManager";
import { QuestRunner } from "../QuestRunner";
import { AreaAssetLoader } from "../loading/AreaAssetLoader";
import { EntityAssetLoader } from "../loading/EntityAssetLoader";
import { property } from "../../core/observable";

/**
 * Model loader used while running a quest.
 */
export class QuestRunnerModelManager extends Quest3DModelManager {
    constructor(
        private readonly quest_runner: QuestRunner,
        renderer: QuestRenderer,
        area_asset_loader: AreaAssetLoader,
        entity_asset_loader: EntityAssetLoader,
    ) {
        super(property(undefined), renderer, area_asset_loader, entity_asset_loader);

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
