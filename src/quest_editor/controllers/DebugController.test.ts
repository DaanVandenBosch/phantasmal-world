import { with_disposer } from "../../../test/src/core/observables/disposable_helpers";
import { GuiStore } from "../../core/stores/GuiStore";
import { create_area_store } from "../../../test/src/quest_editor/stores/store_creation";
import { QuestEditorStore } from "../stores/QuestEditorStore";
import { Episode } from "../../core/data_formats/parsing/quest/Episode";
import { DebugController } from "./DebugController";
import { LogStore } from "../stores/LogStore";
import { create_new_quest } from "../stores/quest_creation";
import { next_animation_frame } from "../../../test/src/utils";

test("Some widgets should only be enabled when a quest is loaded.", async () =>
    with_disposer(async disposer => {
        const gui_store = disposer.add(new GuiStore());
        const area_store = create_area_store(disposer);
        const log_store = disposer.add(new LogStore());
        const quest_editor_store = disposer.add(
            new QuestEditorStore(gui_store, area_store, log_store),
        );
        const ctrl = disposer.add(new DebugController(gui_store, quest_editor_store, log_store));

        expect(ctrl.can_debug.val).toBe(false);
        expect(ctrl.can_step.val).toBe(false);

        await quest_editor_store.set_current_quest(create_new_quest(area_store, Episode.I));

        expect(ctrl.can_debug.val).toBe(true);
        expect(ctrl.can_step.val).toBe(false);
    }));

test("Debugging controls should be enabled and disabled at the right times.", async () =>
    with_disposer(async disposer => {
        const gui_store = disposer.add(new GuiStore());
        const area_store = create_area_store(disposer);
        const log_store = disposer.add(new LogStore());
        const quest_editor_store = disposer.add(
            new QuestEditorStore(gui_store, area_store, log_store),
        );
        const ctrl = disposer.add(new DebugController(gui_store, quest_editor_store, log_store));

        await quest_editor_store.set_current_quest(create_new_quest(area_store, Episode.I));

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
    }));
