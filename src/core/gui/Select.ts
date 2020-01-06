import { LabelledControl, LabelledControlOptions, LabelPosition } from "./LabelledControl";
import { disposable_listener, div, Icon } from "./dom";
import "./Select.css";
import { is_property, Property } from "../observable/property/Property";
import { Button } from "./Button";
import { WritableProperty } from "../observable/property/WritableProperty";
import { WidgetProperty } from "../observable/property/WidgetProperty";
import { Menu } from "./Menu";
import { property } from "../observable";

export type SelectOptions<T> = LabelledControlOptions & {
    readonly items: readonly T[] | Property<readonly T[]>;
    readonly to_label?: (element: T) => string;
    readonly selected?: T | Property<T>;
};

export class Select<T> extends LabelledControl {
    readonly element = div({ className: "core_Select" });

    readonly preferred_label_position: LabelPosition;

    readonly selected: WritableProperty<T | undefined>;

    private readonly items: Property<readonly T[]>;
    private readonly to_label: (element: T) => string;
    private readonly button: Button;
    private readonly menu: Menu<T>;
    private readonly _selected: WidgetProperty<T | undefined>;
    private just_opened: boolean;

    constructor(options: SelectOptions<T>) {
        super(options);

        this.preferred_label_position = "left";

        this.items = is_property(options.items) ? options.items : property(options.items);
        this.to_label = options.to_label ?? String;
        this.button = this.disposable(
            new Button({
                text: " ",
                icon_right: Icon.TriangleDown,
            }),
        );
        this.menu = this.disposable(
            new Menu<T>({
                items: this.items,
                to_label: this.to_label,
                related_element: this.element,
            }),
        );
        this.element.append(this.button.element, this.menu.element);

        this._selected = new WidgetProperty<T | undefined>(this, undefined, this.set_selected);
        this.selected = this._selected;

        this.just_opened = false;

        this.disposables(
            disposable_listener(this.button.element, "mousedown", this.button_mousedown),

            this.button.onmouseup.observe(this.button_mouseup),

            this.button.onkeydown.observe(this.button_keydown),

            this.menu.selected.observe(({ value }) => {
                this._selected.set_val(value, { silent: false });
            }),
        );

        if (options) {
            if (is_property(options.selected)) {
                this.selected.bind_to(options.selected);
            } else if (options.selected) {
                this.selected.val = options.selected;
            }
        }

        this.finalize_construction();
    }

    protected set_enabled(enabled: boolean): void {
        super.set_enabled(enabled);
        this.button.enabled.val = enabled;
    }

    protected set_selected(selected?: T): void {
        this.button.text.val = selected !== undefined ? this.to_label(selected) : " ";
        this.menu.selected.val = selected;
    }

    private button_mousedown = (e: Event): void => {
        e.stopPropagation();
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
        switch (evt.key) {
            case "Enter":
            case " ":
                evt.preventDefault();
                this.just_opened = !this.menu.visible.val;
                this.menu.visible.val = true;
                this.menu.focus();
                this.menu.hover_next();
                break;

            case "ArrowUp":
                {
                    if (this._selected.val === undefined) break;

                    const index = this.items.val.indexOf(this._selected.val) - 1;
                    if (index < 0) break;

                    this._selected.set_val(this.items.val[index], { silent: false });
                }
                break;

            case "ArrowDown":
                {
                    if (this._selected.val === undefined) break;

                    const index = this.items.val.indexOf(this._selected.val) + 1;
                    if (index >= this.items.val.length) break;

                    this._selected.set_val(this.items.val[index], { silent: false });
                }
                break;
        }
    };
}
