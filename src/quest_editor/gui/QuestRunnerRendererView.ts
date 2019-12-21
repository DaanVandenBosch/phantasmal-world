import { QuestRenderer } from "../rendering/QuestRenderer";
import { QuestRunnerModelManager } from "../rendering/QuestRunnerModelManager";
import { QuestRendererView } from "./QuestRendererView";
import { QuestRunner } from "../QuestRunner";
import { GuiStore } from "../../core/stores/GuiStore";
import { QuestEditorStore } from "../stores/QuestEditorStore";

export class QuestRunnerRendererView extends QuestRendererView {
    constructor(gui_store: GuiStore, quest_editor_store: QuestEditorStore) {
        super(
            gui_store,
            quest_editor_store,
            "quest_editor_QuestRunnerRendererView",
            new QuestRenderer(
                renderer => new QuestRunnerModelManager(quest_editor_store.quest_runner, renderer),
            ),
        );

        this.renderer.init_camera_controls();

        this.finalize_construction();
    }
}
