import { Disposable } from "../observable/Disposable";

export abstract class Renderer implements Disposable {
    abstract readonly canvas_element: HTMLCanvasElement;

    abstract dispose(): void;

    abstract start_rendering(): void;

    abstract stop_rendering(): void;

    abstract set_size(width: number, height: number): void;
}
