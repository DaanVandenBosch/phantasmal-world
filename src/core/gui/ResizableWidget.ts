import { Widget } from "./Widget";
import { Resizable } from "./Resizable";

export abstract class ResizableWidget<E extends HTMLElement = HTMLElement> extends Widget<E>
    implements Resizable {
    protected width: number = 0;
    protected height: number = 0;

    resize(width: number, height: number): this {
        this.width = width;
        this.height = height;
        this.element.style.width = `${width}px`;
        this.element.style.height = `${height}px`;
        return this;
    }
}
