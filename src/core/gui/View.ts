import { Disposable } from "../observable/Disposable";
import { Disposer } from "../observable/Disposer";
import { Observable } from "../observable/Observable";
import { bind_hidden } from "./dom";
import { WritableProperty } from "../observable/WritableProperty";
import { property } from "../observable";

export abstract class View implements Disposable {
    abstract readonly element: HTMLElement;

    get id(): string {
        return this.element.id;
    }

    set id(id: string) {
        this.element.id = id;
    }

    readonly visible: WritableProperty<boolean> = property(true);

    private disposer = new Disposer();

    constructor() {
        this.disposables(this.visible.observe(({ value }) => (this.element.hidden = !value)));
    }

    focus(): void {
        this.element.focus();
    }

    dispose(): void {
        this.element.remove();
        this.disposer.dispose();
    }

    protected bind_hidden(element: HTMLElement, observable: Observable<boolean>): void {
        this.disposable(bind_hidden(element, observable));
    }

    protected bind_disabled(element: HTMLElement, observable: Observable<boolean>): void {
        this.disposable(bind_hidden(element, observable));
    }

    protected disposable<T extends Disposable>(disposable: T): T {
        return this.disposer.add(disposable);
    }

    protected disposables(...disposables: Disposable[]): void {
        this.disposer.add_all(...disposables);
    }
}
