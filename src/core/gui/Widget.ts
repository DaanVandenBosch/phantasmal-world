import { Disposable } from "../observable/Disposable";
import { Disposer } from "../observable/Disposer";
import { Observable } from "../observable/Observable";
import { bind_hidden } from "./dom";
import { WritableProperty } from "../observable/property/WritableProperty";
import { WidgetProperty } from "../observable/property/WidgetProperty";
import { Property } from "../observable/property/Property";
import { LogManager } from "../Logger";

const logger = LogManager.get("core/gui/Widget");

export type WidgetOptions = {
    id?: string;
    class?: string;
    enabled?: boolean | Property<boolean>;
    tooltip?: string | Property<string>;
};

export abstract class Widget implements Disposable {
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
    private readonly options: WidgetOptions;
    private construction_finalized = false;

    abstract readonly element: HTMLElement;

    get id(): string {
        return this.element.id;
    }

    set id(id: string) {
        this.element.id = id;
    }

    get disposed(): boolean {
        return this.disposer.disposed;
    }

    readonly visible: WritableProperty<boolean> = this._visible;
    readonly enabled: WritableProperty<boolean> = this._enabled;
    readonly tooltip: WritableProperty<string> = this._tooltip;

    protected constructor(options: WidgetOptions = {}) {
        this.options = options;

        setTimeout(() => {
            if (!this.construction_finalized) {
                logger.error(
                    `finalize_construction is never called for ${
                        Object.getPrototypeOf(this).constructor.name
                    }.`,
                );
            }
        }, 0);
    }

    focus(): void {
        this.element.focus();
    }

    dispose(): void {
        this.element.remove();
        this.disposer.dispose();
    }

    protected finalize_construction(): void {
        if (Object.getPrototypeOf(this) !== this.constructor.prototype) return;

        // At this point we know `this.element` is initialized.
        if (this.options.id) {
            this.element.id = this.options.id;
        }

        if (this.options.class) {
            this.element.classList.add(this.options.class);
        }

        if (typeof this.options.enabled === "boolean") {
            this.enabled.val = this.options.enabled;
        } else if (this.options.enabled) {
            this.enabled.bind_to(this.options.enabled);
        }

        if (typeof this.options.tooltip === "string") {
            this.tooltip.val = this.options.tooltip;
        } else if (this.options.tooltip) {
            this.tooltip.bind_to(this.options.tooltip);
        }

        this.construction_finalized = true;
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
