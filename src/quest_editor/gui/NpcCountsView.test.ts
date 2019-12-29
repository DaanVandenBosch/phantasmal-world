import { NpcCountsView } from "./NpcCountsView";
import { NpcCountsController } from "../controllers/NpcCountsController";
import {
    create_area_store,
    create_quest_editor_store,
} from "../../../test/src/quest_editor/stores/store_creation";
import { with_disposer } from "../../../test/src/core/observables/disposable_helpers";
import { create_new_quest } from "../stores/quest_creation";
import { Episode } from "../../core/data_formats/parsing/quest/Episode";

test("Renders correctly without a current quest.", () =>
    with_disposer(disposer => {
        const store = create_quest_editor_store(disposer);
        const view = disposer.add(new NpcCountsView(disposer.add(new NpcCountsController(store))));

        expect(view.element).toMatchSnapshot('Should render a "No quest loaded." view.');
    }));

test("Renders correctly with a current quest.", () =>
    with_disposer(disposer => {
        const area_store = create_area_store(disposer);
        const store = create_quest_editor_store(disposer, area_store);
        const view = disposer.add(new NpcCountsView(disposer.add(new NpcCountsController(store))));

        store.set_current_quest(create_new_quest(area_store, Episode.I));

        expect(view.element).toMatchSnapshot("Should render a table with NPC names and counts.");
    }));
