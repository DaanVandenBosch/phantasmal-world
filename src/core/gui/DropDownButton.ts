import { disposable_listener, el, Icon } from "./dom";
import "./DropDownButton.css";
import { Property } from "../observable/property/Property";
import { Button, ButtonOptions } from "./Button";
import { Menu } from "./Menu";
import { Control } from "./Control";
import { Observable } from "../observable/Observable";
import { Emitter } from "../observable/Emitter";
import { emitter } from "../observable";

export type DropDownButtonOptions = ButtonOptions;

export class DropDownButton<T> extends Control {
    readonly chosen: Observable<T>;

    private readonly button: Button;
    private readonly menu: Menu<T>;
    private readonly _chosen: Emitter<T>;
    private just_opened: boolean;

    constructor(
        text: string,
        items: T[] | Property<T[]>,
        to_label: (element: T) => string,
        options?: DropDownButtonOptions,
    ) {
        const button = new Button(text, {
            icon_left: options && options.icon_left,
            icon_right: Icon.TriangleDown,
        });
        const menu = new Menu<T>(items, to_label);

        super(el.div({ class: "core_DropDownButton" }, button.element, menu.element), options);

        this.button = this.disposable(button);
        this.menu = this.disposable(menu);

        this._chosen = emitter();
        this.chosen = this._chosen;

        this.just_opened = false;

        this.disposables(
            disposable_listener(button.element, "mousedown", e => this.button_mousedown(e)),

            button.mouseup.observe(() => this.button_mouseup()),

            this.menu.selected.observe(({ value }) => {
                if (value) {
                    this._chosen.emit({ value });
                    this.menu.selected.val = undefined;
                }
            }),
        );
    }

    protected set_enabled(enabled: boolean): void {
        super.set_enabled(enabled);
        this.button.enabled.val = enabled;
    }

    private button_mousedown(e: Event): void {
        e.stopPropagation();
        this.just_opened = !this.menu.visible.val;
        this.menu.visible.val = true;
    }

    private button_mouseup(): void {
        if (!this.just_opened) {
            this.menu.visible.val = false;
        }

        this.just_opened = false;
    }
}
