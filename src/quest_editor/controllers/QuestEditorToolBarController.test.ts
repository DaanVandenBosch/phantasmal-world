import { GuiStore } from "../../core/stores/GuiStore";
import {
    create_area_store,
    create_quest_editor_store,
} from "../../../test/src/quest_editor/stores/store_creation";
import { QuestEditorToolBarController } from "./QuestEditorToolBarController";
import { Episode } from "../../core/data_formats/parsing/quest/Episode";
import { QuestLoader } from "../loading/QuestLoader";
import { FileSystemHttpClient } from "../../../test/src/core/FileSystemHttpClient";
import { pw_test } from "../../../test/src/utils";

test(
    "Some widgets should only be enabled when a quest is loaded.",
    pw_test({}, async disposer => {
        const quest_loader = disposer.add(new QuestLoader(new FileSystemHttpClient()));
        const gui_store = disposer.add(new GuiStore());
        const area_store = create_area_store(disposer);
        const quest_editor_store = create_quest_editor_store(disposer, area_store);
        const ctrl = disposer.add(
            new QuestEditorToolBarController(
                quest_loader,
                gui_store,
                area_store,
                quest_editor_store,
            ),
        );

        expect(ctrl.can_save.val).toBe(false);
        expect(ctrl.can_select_area.val).toBe(false);

        await ctrl.create_new_quest(Episode.I);

        expect(ctrl.can_save.val).toBe(true);
        expect(ctrl.can_select_area.val).toBe(true);
    }),
);
