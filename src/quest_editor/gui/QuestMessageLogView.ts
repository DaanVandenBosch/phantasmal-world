import { ResizableWidget } from "../../core/gui/ResizableWidget";
import { quest_editor_store } from "../stores/QuestEditorStore";
import { MessageLog } from "../../core/gui/MessageLog";
import "./QuestMessageLogView.css";

export class QuestMessageLogView extends MessageLog {
    constructor() {
        super(quest_editor_store, "quest_editor_QuestMessageLogView");
    }
}
