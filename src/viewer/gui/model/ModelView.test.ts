import { with_disposer } from "../../../../test/src/core/observables/disposable_helpers";
import { ModelController } from "../../controllers/model/ModelController";
import { CharacterClassAssetLoader } from "../../loading/CharacterClassAssetLoader";
import { FileSystemHttpClient } from "../../../../test/src/core/FileSystemHttpClient";
import { ModelView } from "./ModelView";
import { ModelRenderer } from "../../rendering/ModelRenderer";
import { StubThreeRenderer } from "../../../../test/src/core/rendering/StubThreeRenderer";
import { Random } from "../../../core/Random";
import { ModelStore } from "../../stores/ModelStore";
import { ModelToolBarView } from "./ModelToolBarView";
import { ModelToolBarController } from "../../controllers/model/ModelToolBarController";
import { CharacterClassOptionsView } from "./CharacterClassOptionsView";
import { CharacterClassOptionsController } from "../../controllers/model/CharacterClassOptionsController";

test("Renders correctly.", () =>
    with_disposer(disposer => {
        const store = disposer.add(
            new ModelStore(
                disposer.add(new CharacterClassAssetLoader(new FileSystemHttpClient())),
                new Random(() => 0.04),
            ),
        );
        const view = new ModelView(
            disposer.add(new ModelController(store)),
            new ModelToolBarView(disposer.add(new ModelToolBarController(store))),
            new CharacterClassOptionsView(disposer.add(new CharacterClassOptionsController(store))),
            new ModelRenderer(store, new StubThreeRenderer()),
        );

        expect(view.element).toMatchSnapshot();
    }));
