import {
    create_area_store,
    create_quest_editor_store,
} from "../../../test/src/quest_editor/stores/store_creation";
import { QuestInfoController } from "./QuestInfoController";
import { load_default_quest_model, pw_test } from "../../../test/src/utils";

test(
    "When a property's input value changes, this should be reflected in the current quest object and the undo stack.",
    pw_test({}, async disposer => {
        const area_store = create_area_store(disposer);
        const store = create_quest_editor_store(disposer, area_store);
        const ctrl = disposer.add(new QuestInfoController(store));

        await store.set_current_quest(load_default_quest_model(area_store));

        ctrl.set_id(3004);
        expect(store.current_quest.val!.id.val).toBe(3004);
        expect(store.undo.undo()).toBe(true);
        expect(store.current_quest.val!.id.val).toBe(0);

        ctrl.set_name("Correct Horse Battery Staple");
        expect(store.current_quest.val!.name.val).toBe("Correct Horse Battery Staple");
        expect(store.undo.undo()).toBe(true);
        expect(store.current_quest.val!.name.val).toBe("Untitled");

        ctrl.set_short_description("This is a short description.");
        expect(store.current_quest.val!.short_description.val).toBe("This is a short description.");
        expect(store.undo.undo()).toBe(true);
        expect(store.current_quest.val!.short_description.val).toBe(
            "Created with phantasmal.world.",
        );

        ctrl.set_long_description("This is a somewhat longer description.");
        expect(store.current_quest.val!.long_description.val).toBe(
            "This is a somewhat longer description.",
        );
        expect(store.undo.undo()).toBe(true);
        expect(store.current_quest.val!.long_description.val).toBe(
            "Created with phantasmal.world.",
        );
    }),
);
