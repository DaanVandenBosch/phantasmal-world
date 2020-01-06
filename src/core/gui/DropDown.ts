import { disposable_listener, div, Icon } from "./dom";
import "./DropDown.css";
import { Property } from "../observable/property/Property";
import { Button, ButtonOptions } from "./Button";
import { Menu } from "./Menu";
import { Control } from "./Control";
import { Observable } from "../observable/Observable";
import { Emitter } from "../observable/Emitter";
import { emitter } from "../observable";

export type DropDownOptions<T> = ButtonOptions & {
    readonly text: string;
    readonly items: readonly T[] | Property<readonly T[]>;
    readonly to_label?: (element: T) => string;
};

export class DropDown<T> extends Control {
    readonly element = div({ className: "core_DropDown" });

    readonly chosen: Observable<T>;

    private readonly button: Button;
    private readonly menu: Menu<T>;
    private readonly _chosen: Emitter<T>;
    private just_opened: boolean;

    constructor(options: DropDownOptions<T>) {
        super(options);

        this.button = this.disposable(
            new Button({
                text: options.text,
                icon_left: options && options.icon_left,
                icon_right: Icon.TriangleDown,
            }),
        );
        this.menu = this.disposable(
            new Menu({
                items: options.items,
                to_label: options.to_label,
                related_element: this.element,
            }),
        );
        this.element.append(this.button.element, this.menu.element);

        this._chosen = emitter();
        this.chosen = this._chosen;

        this.just_opened = false;

        this.disposables(
            disposable_listener(this.button.element, "mousedown", this.button_mousedown, {
                capture: true,
            }),

            this.button.onmouseup.observe(this.button_mouseup),

            this.button.onkeydown.observe(this.button_keydown),

            this.menu.selected.observe(({ value }) => {
                if (value !== undefined) {
                    this._chosen.emit({ value });
                    this.menu.selected.val = undefined;
                }
            }),
        );

        this.finalize_construction();
    }

    protected set_enabled(enabled: boolean): void {
        super.set_enabled(enabled);
        this.button.enabled.val = enabled;
    }

    private button_mousedown = (): void => {
        this.just_opened = !this.menu.visible.val;
        this.menu.visible.val = true;
    };

    private button_mouseup = (): void => {
        if (this.just_opened) {
            this.menu.focus();
        } else {
            this.menu.visible.val = false;
        }

        this.just_opened = false;
    };

    private button_keydown = ({ value: evt }: { value: KeyboardEvent }): void => {
        if (evt.key === "Enter" || evt.key === " ") {
            evt.preventDefault();
            this.just_opened = !this.menu.visible.val;
            this.menu.visible.val = true;
            this.menu.focus();
            this.menu.hover_next();
        }
    };
}
