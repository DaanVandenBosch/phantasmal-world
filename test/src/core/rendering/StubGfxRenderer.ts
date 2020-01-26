import { GfxRenderer } from "../../../../src/core/rendering/GfxRenderer";
import { Gfx } from "../../../../src/core/rendering/Gfx";

export class StubGfxRenderer extends GfxRenderer {
    get gfx(): Gfx {
        throw new Error("gfx is not implemented.");
    }

    constructor() {
        super(false);
    }

    protected render(): void {} // eslint-disable-line
}
