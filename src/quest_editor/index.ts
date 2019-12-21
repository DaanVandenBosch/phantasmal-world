import { QuestEditorView } from "./gui/QuestEditorView";
import { GuiStore } from "../core/stores/GuiStore";
import { QuestEditorStore } from "./stores/QuestEditorStore";
import { AsmEditorStore } from "./stores/AsmEditorStore";

export function initialize_quest_editor(gui_store: GuiStore): QuestEditorView {
    const quest_editor_store = new QuestEditorStore(gui_store);
    const asm_editor_store = new AsmEditorStore(quest_editor_store);

    return new QuestEditorView(gui_store, quest_editor_store, asm_editor_store);
}
