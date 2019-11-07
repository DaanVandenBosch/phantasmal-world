import { disposable_listener, el, Icon } from "./dom";
import "./DropDown.css";
import { Property } from "../observable/property/Property";
import { Button, ButtonOptions } from "./Button";
import { Menu } from "./Menu";
import { Control } from "./Control";
import { Observable } from "../observable/Observable";
import { Emitter } from "../observable/Emitter";
import { emitter } from "../observable";

export type DropDownOptions = ButtonOptions;

export class DropDown<T> extends Control {
    readonly element = el.div({ class: "core_DropDown" });

    readonly chosen: Observable<T>;

    private readonly button: Button;
    private readonly menu: Menu<T>;
    private readonly _chosen: Emitter<T>;
    private just_opened: boolean;

    constructor(
        text: string,
        items: readonly T[] | Property<readonly T[]>,
        to_label: (element: T) => string,
        options?: DropDownOptions,
    ) {
        super(options);

        this.button = this.disposable(
            new Button(text, {
                icon_left: options && options.icon_left,
                icon_right: Icon.TriangleDown,
            }),
        );
        this.menu = this.disposable(new Menu<T>(items, to_label, this.element));
        this.element.append(this.button.element, this.menu.element);

        this._chosen = emitter();
        this.chosen = this._chosen;

        this.just_opened = false;

        this.disposables(
            disposable_listener(this.button.element, "mousedown", () => this.button_mousedown(), {
                capture: true,
            }),

            this.button.mouseup.observe(() => this.button_mouseup()),

            this.menu.selected.observe(({ value }) => {
                if (value) {
                    this._chosen.emit({ value });
                    this.menu.selected.val = undefined;
                }
            }),
        );

        this.finalize_construction(DropDown.prototype);
    }

    protected set_enabled(enabled: boolean): void {
        super.set_enabled(enabled);
        this.button.enabled.val = enabled;
    }

    private button_mousedown(): void {
        this.just_opened = !this.menu.visible.val;
        this.menu.visible.val = true;
    }

    private button_mouseup(): void {
        if (this.just_opened) {
            this.menu.focus();
        } else {
            this.menu.visible.val = false;
        }

        this.just_opened = false;
    }
}
