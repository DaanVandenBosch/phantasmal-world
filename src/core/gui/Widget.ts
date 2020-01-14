import { Disposable } from "../observable/Disposable";
import { Disposer } from "../observable/Disposer";
import { WritableProperty } from "../observable/property/WritableProperty";
import { WidgetProperty } from "../observable/property/WidgetProperty";
import { Property } from "../observable/property/Property";
import { LogManager } from "../Logger";

const logger = LogManager.get("core/gui/Widget");

export type WidgetOptions = {
    readonly id?: string;
    readonly class?: string;
    readonly visible?: boolean | Property<boolean>;
    readonly enabled?: boolean | Property<boolean>;
    readonly tooltip?: string | Property<string>;
};

/**
 * A user interface element.
 */
export abstract class Widget implements Disposable {
    private readonly disposer = new Disposer();

    private _active = false;

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

    /**
     * An active widget might, for example, run an animation loop.
     */
    get active(): boolean {
        return this._active;
    }

    abstract readonly children: readonly Widget[];

    get disposed(): boolean {
        return this.disposer.disposed;
    }

    /**
     * An invisible widget typically sets the hidden attribute on its {@link element}.
     */
    readonly visible: WritableProperty<boolean> = this._visible;
    /**
     * A disabled widget typically sets the disabled attribute on its {@link element} and adds the
     * `disabled` class to it.
     */
    readonly enabled: WritableProperty<boolean> = this._enabled;
    /**
     * The {@link tooltip} property typically corresponds to the `tooltip` attribute of its
     * {@link element}.
     */
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

    /**
     * Activate this widget. This call will also be propagated to the relevant children.
     */
    activate(): void {
        this._active = true;

        for (const child of this.children) {
            child.activate();
        }
    }

    /**
     * Deactivate this widget. This call will also be propagated to the relevant children.
     */
    deactivate(): void {
        this._active = false;

        for (const child of this.children) {
            child.deactivate();
        }
    }

    /**
     * Move focus to this widget.
     */
    focus(): void {
        this.element.focus();
    }

    /**
     * Removes the widget's {@link element} from the DOM and disposes all its held disposables.
     */
    dispose(): void {
        this.element.remove();
        this.disposer.dispose();
    }

    /**
     * Every concrete subclass of {@link Widget} should call this method at the end of its
     * constructor. When this method is called, we can refer to abstract properties that are
     * provided by subclasses.
     */
    protected finalize_construction(): void {
        if (Object.getPrototypeOf(this) !== this.constructor.prototype) return;

        // At this point we know `this.element` is initialized.
        if (this.options.id) {
            this.element.id = this.options.id;
        }

        if (this.options.class) {
            this.element.classList.add(this.options.class);
        }

        if (typeof this.options.visible === "boolean") {
            this.visible.val = this.options.visible;
        } else if (this.options.visible) {
            this.visible.bind_to(this.options.visible);
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

    protected disposable<T extends Disposable>(disposable: T): T {
        return this.disposer.add(disposable);
    }

    protected disposables(...disposables: Disposable[]): void {
        this.disposer.add_all(...disposables);
    }

    protected remove_disposable(disposable: Disposable): void {
        this.disposer.remove(disposable);
    }
}
