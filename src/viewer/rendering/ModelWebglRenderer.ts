import { WebglRenderer } from "../../core/rendering/webgl/WebglRenderer";
import { ModelStore } from "../stores/ModelStore";
import { Disposer } from "../../core/observable/Disposer";

export class ModelWebglRenderer extends WebglRenderer {
    private readonly disposer = new Disposer();

    constructor(private readonly store: ModelStore) {
        super();
    }

    dispose(): void {
        super.dispose();
        this.disposer.dispose();
    }
}
