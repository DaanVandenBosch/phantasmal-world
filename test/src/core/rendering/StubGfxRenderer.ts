import { GfxRenderer } from "../../../../src/core/rendering/GfxRenderer";
import { Gfx } from "../../../../src/core/rendering/Gfx";
import { Projection } from "../../../../src/core/rendering/Camera";

export class StubGfxRenderer extends GfxRenderer {
    get gfx(): Gfx {
        throw new Error("gfx is not implemented.");
    }

    constructor() {
        super(document.createElement("canvas"), Projection.Orthographic);
    }

    protected render(): void {} // eslint-disable-line
}
