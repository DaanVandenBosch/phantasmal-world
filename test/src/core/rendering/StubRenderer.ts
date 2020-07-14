import { DisposableThreeRenderer } from "../../../../src/core/rendering/Renderer";

export const STUB_RENDERER: DisposableThreeRenderer = {
    domElement: document.createElement("canvas"),

    dispose(): void {}, // eslint-disable-line

    render(): void {}, // eslint-disable-line

    setSize(): void {}, // eslint-disable-line
} as any;