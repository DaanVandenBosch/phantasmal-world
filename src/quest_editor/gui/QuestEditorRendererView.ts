import { QuestRenderer } from "../rendering/QuestRenderer";
import { QuestEditorStore } from "../stores/QuestEditorStore";
import { QuestEditor3DModelManager } from "../rendering/QuestEditor3DModelManager";
import { QuestRendererView } from "./QuestRendererView";
import { QuestEntityControls } from "../rendering/QuestEntityControls";
import { GuiStore } from "../../core/stores/GuiStore";
import { AreaAssetLoader } from "../loading/AreaAssetLoader";
import { EntityAssetLoader } from "../loading/EntityAssetLoader";
import { DisposableThreeRenderer } from "../../core/rendering/Renderer";

export class QuestEditorRendererView extends QuestRendererView {
    private readonly entity_controls: QuestEntityControls;

    constructor(
        gui_store: GuiStore,
        quest_editor_store: QuestEditorStore,
        area_asset_loader: AreaAssetLoader,
        entity_asset_loader: EntityAssetLoader,
        three_renderer: DisposableThreeRenderer,
    ) {
        super(
            gui_store,
            quest_editor_store,
            "quest_editor_QuestEditorRendererView",
            new QuestRenderer(
                three_renderer,
                renderer =>
                    new QuestEditor3DModelManager(
                        quest_editor_store.current_quest,
                        quest_editor_store.current_area,
                        quest_editor_store.current_wave,
                        renderer,
                        area_asset_loader,
                        entity_asset_loader,
                    ),
            ),
        );

        this.element.addEventListener("focus", () => quest_editor_store.undo.make_current(), true);

        this.entity_controls = this.disposable(
            new QuestEntityControls(quest_editor_store, this.renderer),
        );

        this.disposables(
            quest_editor_store.selected_entity.observe(
                ({ value }) => (this.renderer.selected_entity = value),
            ),

            quest_editor_store.quest_runner.running.observe(
                ({ value: running }) => (this.entity_controls.enabled = !running),
                { call_now: true },
            ),
        );

        this.renderer.init_camera_controls();

        this.finalize_construction();
    }
}
