import { Disposable } from "../observable/Disposable";
import { Disposer } from "../observable/Disposer";
import { Observable } from "../observable/Observable";
import { bind_hidden } from "./dom";
import { WritableProperty } from "../observable/property/WritableProperty";
import { WidgetProperty } from "../observable/property/WidgetProperty";
import { Property } from "../observable/property/Property";

export type WidgetOptions = {
    class?: string;
    enabled?: boolean | Property<boolean>;
    tooltip?: string | Property<string>;
};

export abstract class Widget<E extends HTMLElement = HTMLElement> implements Disposable {
    readonly element: E;

    get id(): string {
        return this.element.id;
    }

    set id(id: string) {
        this.element.id = id;
    }

    readonly visible: WritableProperty<boolean>;
    readonly enabled: WritableProperty<boolean>;
    readonly tooltip: WritableProperty<string>;

    protected disposed = false;

    private readonly disposer = new Disposer();
    private readonly _visible: WidgetProperty<boolean> = new WidgetProperty<boolean>(
        this,
        true,
        this.set_visible,
    );
    private readonly _enabled: WidgetProperty<boolean> = new WidgetProperty<boolean>(
        this,
        true,
        this.set_enabled,
    );
    private readonly _tooltip: WidgetProperty<string> = new WidgetProperty<string>(
        this,
        "",
        this.set_tooltip,
    );

    protected constructor(element: E, options?: WidgetOptions) {
        this.element = element;
        this.visible = this._visible;
        this.enabled = this._enabled;
        this.tooltip = this._tooltip;

        if (options) {
            if (options.class) {
                this.element.classList.add(options.class);
            }

            if (typeof options.enabled === "boolean") {
                this.enabled.val = options.enabled;
            } else if (options.enabled) {
                this.enabled.bind_to(options.enabled);
            }

            if (typeof options.tooltip === "string") {
                this.tooltip.val = options.tooltip;
            } else if (options.tooltip) {
                this.tooltip.bind_to(options.tooltip);
            }
        }
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

    protected set_tooltip(tooltip: string): void {
        this.element.title = tooltip;
    }

    protected bind_hidden(element: HTMLElement, observable: Observable<boolean>): void {
        this.disposable(bind_hidden(element, observable));
    }

    protected disposable<T extends Disposable>(disposable: T): T {
        return this.disposer.add(disposable);
    }

    protected disposables(...disposables: Disposable[]): void {
        this.disposer.add_all(...disposables);
    }
}
