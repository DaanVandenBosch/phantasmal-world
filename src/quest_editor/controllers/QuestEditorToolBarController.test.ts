import { GuiStore } from "../../core/stores/GuiStore";
import { create_area_store } from "../../../test/src/quest_editor/stores/store_creation";
import { QuestEditorStore } from "../stores/QuestEditorStore";
import { QuestEditorToolBarController } from "./QuestEditorToolBarController";
import { Episode } from "../../core/data_formats/parsing/quest/Episode";
import { next_animation_frame } from "../../../test/src/utils";

test("Some widgets should only be enabled when a quest is loaded.", async () => {
    const gui_store = new GuiStore();
    const area_store = create_area_store();
    const quest_editor_store = new QuestEditorStore(gui_store, area_store);
    const ctrl = new QuestEditorToolBarController(gui_store, area_store, quest_editor_store);

    expect(ctrl.can_save.val).toBe(false);
    expect(ctrl.can_debug.val).toBe(false);
    expect(ctrl.can_select_area.val).toBe(false);
    expect(ctrl.can_step.val).toBe(false);

    await ctrl.create_new_quest(Episode.I);

    expect(ctrl.can_save.val).toBe(true);
    expect(ctrl.can_debug.val).toBe(true);
    expect(ctrl.can_select_area.val).toBe(true);
    expect(ctrl.can_step.val).toBe(false);
});

test("Debugging controls should be enabled and disabled at the right times.", async () => {
    const gui_store = new GuiStore();
    const area_store = create_area_store();
    const quest_editor_store = new QuestEditorStore(gui_store, area_store);
    const ctrl = new QuestEditorToolBarController(gui_store, area_store, quest_editor_store);

    await ctrl.create_new_quest(Episode.I);

    try {
        expect(ctrl.can_step.val).toBe(false);
        expect(ctrl.can_stop.val).toBe(false);

        ctrl.debug();
        await next_animation_frame();

        expect(ctrl.can_step.val).toBe(false);
        expect(ctrl.can_stop.val).toBe(true);

        ctrl.stop();

        expect(ctrl.can_step.val).toBe(false);
        expect(ctrl.can_stop.val).toBe(false);

        quest_editor_store.quest_runner.set_breakpoint(5);
        ctrl.debug();
        await next_animation_frame();

        expect(ctrl.can_step.val).toBe(true);
        expect(ctrl.can_stop.val).toBe(true);
    } finally {
        ctrl.stop();
    }
});
