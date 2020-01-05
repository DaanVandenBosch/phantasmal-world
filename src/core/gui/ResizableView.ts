import { View } from "./View";
import { Resizable } from "./Resizable";

export abstract class ResizableView extends View implements Resizable {
    protected width: number = 0;
    protected height: number = 0;

    resize(width: number, height: number): void {
        this.width = width;
        this.height = height;
        this.element.style.width = `${width}px`;
        this.element.style.height = `${height}px`;
    }
}
