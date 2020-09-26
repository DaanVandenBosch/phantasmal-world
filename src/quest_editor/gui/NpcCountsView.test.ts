import { NpcCountsView } from "./NpcCountsView";
import { NpcCountsController } from "../controllers/NpcCountsController";
import {
    create_area_store,
    create_quest_editor_store,
} from "../../../test/src/quest_editor/stores/store_creation";
import { load_default_quest_model, pw_test } from "../../../test/src/utils";

test(
    "Renders correctly without a current quest.",
    pw_test({}, disposer => {
        const store = create_quest_editor_store(disposer);
        const view = disposer.add(new NpcCountsView(disposer.add(new NpcCountsController(store))));

        expect(view.element).toMatchSnapshot('Should render a "No quest loaded." view.');
    }),
);

test(
    "Renders correctly with a current quest.",
    pw_test({}, disposer => {
        const area_store = create_area_store(disposer);
        const store = create_quest_editor_store(disposer, area_store);
        const view = disposer.add(new NpcCountsView(disposer.add(new NpcCountsController(store))));

        store.set_current_quest(load_default_quest_model(area_store));

        expect(view.element).toMatchSnapshot("Should render a table with NPC names and counts.");
    }),
);
