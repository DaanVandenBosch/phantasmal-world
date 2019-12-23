/**
 * @jest-environment jsdom
 */
import { QuestInfoController } from "../controllers/QuestInfoController";
import { undo_manager } from "../../core/undo/UndoManager";
import { QuestInfoView } from "./QuestInfoView";
import { Episode } from "../../core/data_formats/parsing/quest/Episode";
import { create_quest_editor_store } from "../../../test/src/quest_editor/stores/create_quest_editor_store";

test("Renders correctly without a current quest.", () => {
    const view = new QuestInfoView(new QuestInfoController(create_quest_editor_store()));

    expect(view.element).toMatchSnapshot('should render a "No quest loaded." view');
});

test("Renders correctly with a current quest.", () => {
    const store = create_quest_editor_store();
    const view = new QuestInfoView(new QuestInfoController(store));
    store.new_quest(Episode.I);

    expect(view.element).toMatchSnapshot("should render property inputs");
});

test("When its element is focused the store's undo stack should become the current stack.", () => {
    const store = create_quest_editor_store();
    const view = new QuestInfoView(new QuestInfoController(store));

    undo_manager.make_noop_current();

    view.element.focus();

    expect(undo_manager.current.val).toBe(store.undo);
});

test("When a property's input value changes, this should be reflected in the current quest object.", async () => {
    const store = create_quest_editor_store();
    const view = new QuestInfoView(new QuestInfoController(store));

    await store.new_quest(Episode.I);

    for (const [prop, value] of [
        ["id", 3004],
        ["name", "Correct Horse Battery Staple"],
        ["short_description", "This is a short description."],
        ["long_description", "This is a somewhat longer description."],
    ]) {
        const input = view.element.querySelector(
            `#quest_editor_QuestInfoView_${prop} input, #quest_editor_QuestInfoView_${prop} textarea`,
        ) as HTMLInputElement;

        input.value = String(value);
        input.dispatchEvent(new Event("change"));

        expect((store.current_quest.val as any)[prop].val).toBe(value);
    }
});
