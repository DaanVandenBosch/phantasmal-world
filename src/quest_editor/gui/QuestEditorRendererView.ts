import { QuestRenderer } from "../rendering/QuestRenderer";
import { quest_editor_store } from "../stores/QuestEditorStore";
import { QuestEditorModelManager } from "../rendering/QuestEditorModelManager";
import { QuestRendererView } from "./QuestRendererView";
import { QuestEntityControls } from "../rendering/QuestEntityControls";

export class QuestEditorRendererView extends QuestRendererView {
    private readonly entity_controls: QuestEntityControls;

    constructor() {
        super("quest_editor_QuestEditorRendererView", new QuestRenderer(QuestEditorModelManager));

        this.element.addEventListener("focus", () => quest_editor_store.undo.make_current(), true);

        this.entity_controls = this.disposable(new QuestEntityControls(this.renderer));

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
