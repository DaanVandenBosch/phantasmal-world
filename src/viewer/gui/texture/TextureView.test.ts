import { TextureView } from "./TextureView";
import { with_disposer } from "../../../../test/src/core/observables/disposable_helpers";
import { TextureController } from "../../controllers/texture/TextureController";
import { TextureRenderer } from "../../rendering/TextureRenderer";
import { STUB_RENDERER } from "../../../../test/src/core/rendering/StubRenderer";

test("Renders correctly without textures.", () =>
    with_disposer(disposer => {
        const ctrl = disposer.add(new TextureController());
        const view = disposer.add(new TextureView(ctrl, new TextureRenderer(ctrl, STUB_RENDERER)));

        expect(view.element).toMatchSnapshot("Should render a toolbar and a renderer widget.");
    }));
