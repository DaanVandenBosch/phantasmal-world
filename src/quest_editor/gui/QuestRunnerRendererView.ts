import { QuestRenderer } from "../rendering/QuestRenderer";
import { QuestRunnerModelManager } from "../rendering/QuestRunnerModelManager";
import { QuestRendererView } from "./QuestRendererView";
import { GuiStore } from "../../core/stores/GuiStore";
import { QuestEditorStore } from "../stores/QuestEditorStore";
import { AreaAssetLoader } from "../loading/AreaAssetLoader";
import { EntityAssetLoader } from "../loading/EntityAssetLoader";

export class QuestRunnerRendererView extends QuestRendererView {
    constructor(
        gui_store: GuiStore,
        quest_editor_store: QuestEditorStore,
        area_asset_loader: AreaAssetLoader,
        entity_asset_loader: EntityAssetLoader,
    ) {
        super(
            gui_store,
            quest_editor_store,
            "quest_editor_QuestRunnerRendererView",
            new QuestRenderer(
                renderer =>
                    new QuestRunnerModelManager(
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
