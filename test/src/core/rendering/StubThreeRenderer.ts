import { DisposableThreeRenderer } from "../../../../src/core/rendering/ThreeRenderer";

export class StubThreeRenderer implements DisposableThreeRenderer {
    domElement: HTMLCanvasElement = document.createElement("canvas");

    dispose(): void {} // eslint-disable-line

    render(): void {} // eslint-disable-line

    setSize(): void {} // eslint-disable-line
}
