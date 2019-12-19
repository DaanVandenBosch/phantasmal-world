import { QuestRenderer } from "../rendering/QuestRenderer";
import { QuestRunnerModelManager } from "../rendering/QuestRunnerModelManager";
import { QuestRendererView } from "./QuestRendererView";

export class QuestRunnerRendererView extends QuestRendererView {
    constructor() {
        super("quest_editor_QuestRunnerRendererView", new QuestRenderer(QuestRunnerModelManager));

        this.renderer.init_camera_controls();

        this.finalize_construction();
    }
}
