/**
 * @jest-environment jsdom
 */
import { QuestEditorToolBarController } from "../controllers/QuestEditorToolBarController";
import { QuestEditorToolBar } from "./QuestEditorToolBar";
import { GuiStore } from "../../core/stores/GuiStore";
import { create_area_store } from "../../../test/src/quest_editor/stores/store_creation";
import { QuestEditorStore } from "../stores/QuestEditorStore";

test("Renders correctly.", () => {
    const gui_store = new GuiStore();
    const area_store = create_area_store();
    const quest_editor_store = new QuestEditorStore(gui_store, area_store);
    const tool_bar = new QuestEditorToolBar(
        new QuestEditorToolBarController(gui_store, area_store, quest_editor_store),
    );

    expect(tool_bar.element).toMatchSnapshot();
});
