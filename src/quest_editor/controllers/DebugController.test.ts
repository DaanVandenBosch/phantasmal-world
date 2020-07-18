import { with_disposer } from "../../../test/src/core/observables/disposable_helpers";
import { GuiStore } from "../../core/stores/GuiStore";
import { create_area_store } from "../../../test/src/quest_editor/stores/store_creation";
import { QuestEditorStore } from "../stores/QuestEditorStore";
import { DebugController } from "./DebugController";
import { LogStore } from "../stores/LogStore";
import { load_default_quest_model, next_animation_frame } from "../../../test/src/utils";
import { disassemble } from "../scripting/disassembly";
import { assemble } from "../scripting/assembly";

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

        await quest_editor_store.set_current_quest(load_default_quest_model(area_store));

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

        const quest = load_default_quest_model(area_store);
        // Disassemble and reassemble the IR to ensure we have source locations in the final IR.
        quest.object_code.splice(
            0,
            Infinity,
            ...assemble(disassemble(quest.object_code)).object_code,
        );

        await quest_editor_store.set_current_quest(quest);

        // Before starting we can't step or stop.
        expect(ctrl.can_step.val).toBe(false);
        expect(ctrl.can_stop.val).toBe(false);

        ctrl.debug();
        await next_animation_frame();

        // When all threads have yielded, all we can do is stop.
        expect(ctrl.can_step.val).toBe(false);
        expect(ctrl.can_stop.val).toBe(true);

        ctrl.stop();

        // After stopping we can't step or stop anymore.
        expect(ctrl.can_step.val).toBe(false);
        expect(ctrl.can_stop.val).toBe(false);

        // After hitting a breakpoint, we can step and stop.
        expect(quest_editor_store.quest_runner.set_breakpoint(5)).toBe(true);

        ctrl.debug();
        await next_animation_frame();

        expect(quest_editor_store.quest_runner.pause_location.val).toBe(5);

        expect(ctrl.can_step.val).toBe(true);
        expect(ctrl.can_stop.val).toBe(true);
    }));
