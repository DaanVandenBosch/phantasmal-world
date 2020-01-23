import { Disposable } from "../observable/Disposable";

export interface Renderer extends Disposable {
    readonly canvas_element: HTMLCanvasElement;

    start_rendering(): void;

    stop_rendering(): void;

    set_size(width: number, height: number): void;
}
