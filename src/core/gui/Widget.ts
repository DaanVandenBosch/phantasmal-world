import { Disposable } from "../observable/Disposable";
import { Disposer } from "../observable/Disposer";
import { Observable } from "../observable/Observable";
import { bind_hidden } from "./dom";
import { WritableProperty } from "../observable/WritableProperty";
import { WidgetProperty } from "../observable/WidgetProperty";

export type ViewOptions = {};

export abstract class Widget implements Disposable {
    abstract readonly element: HTMLElement;

    get id(): string {
        return this.element.id;
    }

    set id(id: string) {
        this.element.id = id;
    }

    readonly visible: WritableProperty<boolean>;
    readonly enabled: WritableProperty<boolean>;

    protected disposed = false;

    private disposer = new Disposer();
    private _visible = new WidgetProperty<boolean>(this, true, this.set_visible);
    private _enabled = new WidgetProperty<boolean>(this, true, this.set_enabled);

    constructor(_options?: ViewOptions) {
        this.visible = this._visible;
        this.enabled = this._enabled;
    }

    focus(): void {
        this.element.focus();
    }

    dispose(): void {
        this.element.remove();
        this.disposer.dispose();
        this.disposed = true;
    }

    protected set_visible(visible: boolean): void {
        this.element.hidden = !visible;
    }

    protected set_enabled(enabled: boolean): void {
        if (enabled) {
            this.element.classList.remove("disabled");
        } else {
            this.element.classList.add("disabled");
        }
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
