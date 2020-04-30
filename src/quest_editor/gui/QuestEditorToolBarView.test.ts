import { QuestEditorToolBarController } from "../controllers/QuestEditorToolBarController";
import { QuestEditorToolBarView } from "./QuestEditorToolBarView";
import { GuiStore } from "../../core/stores/GuiStore";
import { create_area_store } from "../../../test/src/quest_editor/stores/store_creation";
import { QuestEditorStore } from "../stores/QuestEditorStore";
import { with_disposer } from "../../../test/src/core/observables/disposable_helpers";
import { LogStore } from "../stores/LogStore";

test("Renders correctly.", () =>
    with_disposer(disposer => {
        const gui_store = disposer.add(new GuiStore());
        const area_store = create_area_store(disposer);
        const log_store = disposer.add(new LogStore());
        const quest_editor_store = disposer.add(
            new QuestEditorStore(gui_store, area_store, log_store),
        );
        const tool_bar = disposer.add(
            new QuestEditorToolBarView(
                disposer.add(
                    new QuestEditorToolBarController(gui_store, area_store, quest_editor_store),
                ),
            ),
        );

        expect(tool_bar.element).toMatchSnapshot();
    }));
