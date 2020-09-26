import { TextureView } from "./TextureView";
import { TextureController } from "../../controllers/texture/TextureController";
import { TextureRenderer } from "../../rendering/TextureRenderer";
import { STUB_RENDERER } from "../../../../test/src/core/rendering/StubRenderer";
import { pw_test } from "../../../../test/src/utils";

test(
    "Renders correctly without textures.",
    pw_test({}, disposer => {
        const ctrl = disposer.add(new TextureController());
        const view = disposer.add(new TextureView(ctrl, new TextureRenderer(ctrl, STUB_RENDERER)));

        expect(view.element).toMatchSnapshot("Should render a toolbar and a renderer widget.");
    }),
);
