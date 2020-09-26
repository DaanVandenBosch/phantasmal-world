import { EntityInfoView } from "./EntityInfoView";
import { EntityInfoController } from "../controllers/EntityInfoController";
import {
    create_area_store,
    create_quest_editor_store,
} from "../../../test/src/quest_editor/stores/store_creation";
import { undo_manager } from "../../core/undo/UndoManager";
import { load_default_quest_model, pw_test } from "../../../test/src/utils";

test(
    "Renders correctly without an entity selected.",
    pw_test({}, disposer => {
        const area_store = create_area_store(disposer);
        const store = create_quest_editor_store(disposer, area_store);
        const view = disposer.add(
            new EntityInfoView(disposer.add(new EntityInfoController(store))),
        );

        expect(view.element).toMatchSnapshot('should render a "No entity selected." view');

        store.set_current_quest(load_default_quest_model(area_store));

        expect(view.element).toMatchSnapshot('should render a "No entity selected." view');
    }),
);

test(
    "Renders correctly with an entity selected.",
    pw_test({}, disposer => {
        const area_store = create_area_store(disposer);
        const store = create_quest_editor_store(disposer, area_store);
        const view = disposer.add(
            new EntityInfoView(disposer.add(new EntityInfoController(store))),
        );

        const quest = load_default_quest_model(area_store);
        store.set_current_quest(quest);
        store.set_selected_entity(quest.npcs.get(0));

        expect(view.element).toMatchSnapshot("should render a table of editable properties");
    }),
);

test(
    "When the view's element is focused the quest editor store's undo stack should become the current stack.",
    pw_test({}, disposer => {
        const store = create_quest_editor_store(disposer);
        const view = disposer.add(
            new EntityInfoView(disposer.add(new EntityInfoController(store))),
        );

        // Append view element to DOM to make it focusable.
        document.body.append(view.element);

        undo_manager.make_noop_current();

        view.element.focus();

        expect(undo_manager.current.val).toBe(store.undo);
    }),
);
