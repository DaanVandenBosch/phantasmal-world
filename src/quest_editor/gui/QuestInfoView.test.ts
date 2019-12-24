import { QuestInfoController } from "../controllers/QuestInfoController";
import { undo_manager } from "../../core/undo/UndoManager";
import { QuestInfoView } from "./QuestInfoView";
import { Episode } from "../../core/data_formats/parsing/quest/Episode";
import {
    create_area_store,
    create_quest_editor_store,
} from "../../../test/src/quest_editor/stores/store_creation";
import { create_new_quest } from "../stores/quest_creation";

test("Renders correctly without a current quest.", () => {
    const view = new QuestInfoView(new QuestInfoController(create_quest_editor_store()));

    expect(view.element).toMatchSnapshot('should render a "No quest loaded." view');
});

test("Renders correctly with a current quest.", async () => {
    const area_store = create_area_store();
    const store = create_quest_editor_store();
    const view = new QuestInfoView(new QuestInfoController(store));

    await store.set_quest(create_new_quest(area_store, Episode.I));

    expect(view.element).toMatchSnapshot("should render property inputs");
});

test("When the view's element is focused the store's undo stack should become the current stack.", () => {
    const store = create_quest_editor_store();
    const view = new QuestInfoView(new QuestInfoController(store));

    undo_manager.make_noop_current();

    view.element.focus();

    expect(undo_manager.current.val).toBe(store.undo);
});
