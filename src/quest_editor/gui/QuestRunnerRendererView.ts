import { QuestRenderer } from "../rendering/QuestRenderer";
import { QuestRunner3DModelManager } from "../rendering/QuestRunner3DModelManager";
import { QuestRendererView } from "./QuestRendererView";
import { QuestEditorStore } from "../stores/QuestEditorStore";
import { AreaAssetLoader } from "../loading/AreaAssetLoader";
import { EntityAssetLoader } from "../loading/EntityAssetLoader";
import { DisposableThreeRenderer } from "../../core/rendering/Renderer";

export class QuestRunnerRendererView extends QuestRendererView {
    constructor(
        quest_editor_store: QuestEditorStore,
        area_asset_loader: AreaAssetLoader,
        entity_asset_loader: EntityAssetLoader,
        three_renderer: DisposableThreeRenderer,
    ) {
        super(
            quest_editor_store,
            "quest_editor_QuestRunnerRendererView",
            new QuestRenderer(
                three_renderer,
                renderer =>
                    new QuestRunner3DModelManager(
                        quest_editor_store.quest_runner,
                        renderer,
                        area_asset_loader,
                        entity_asset_loader,
                    ),
            ),
        );

        this.renderer.init_camera_controls();

        this.finalize_construction();
    }
}
