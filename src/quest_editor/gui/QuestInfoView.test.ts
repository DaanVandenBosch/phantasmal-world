import { QuestInfoController } from "../controllers/QuestInfoController";
import { undo_manager } from "../../core/undo/UndoManager";
import { QuestInfoView } from "./QuestInfoView";
import {
    create_area_store,
    create_quest_editor_store,
} from "../../../test/src/quest_editor/stores/store_creation";
import { with_disposer } from "../../../test/src/core/observables/disposable_helpers";
import { load_default_quest_model } from "../../../test/src/utils";

test("Renders correctly without a current quest.", () =>
    with_disposer(disposer => {
        const view = disposer.add(
            new QuestInfoView(
                disposer.add(new QuestInfoController(create_quest_editor_store(disposer))),
            ),
        );

        expect(view.element).toMatchSnapshot('should render a "No quest loaded." view');
    }));

test("Renders correctly with a current quest.", () =>
    with_disposer(async disposer => {
        const area_store = create_area_store(disposer);
        const store = create_quest_editor_store(disposer);
        const view = disposer.add(new QuestInfoView(disposer.add(new QuestInfoController(store))));

        await store.set_current_quest(load_default_quest_model(area_store));

        expect(view.element).toMatchSnapshot("should render property inputs");
    }));

test("When the view's element is focused the quest editor store's undo stack should become the current stack.", () =>
    with_disposer(async disposer => {
        const store = create_quest_editor_store(disposer);
        const view = disposer.add(new QuestInfoView(disposer.add(new QuestInfoController(store))));

        undo_manager.make_noop_current();

        view.element.focus();

        expect(undo_manager.current.val).toBe(store.undo);
    }));
