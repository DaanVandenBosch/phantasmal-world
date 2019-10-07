import { LabelledControl, LabelledControlOptions, LabelPosition } from "./LabelledControl";
import { disposable_listener, el, Icon } from "./dom";
import "./Select.css";
import { is_any_property, Property } from "../observable/property/Property";
import { Button } from "./Button";
import { WritableProperty } from "../observable/property/WritableProperty";
import { WidgetProperty } from "../observable/property/WidgetProperty";
import { Menu } from "./Menu";

export type SelectOptions<T> = LabelledControlOptions & {
    selected?: T | Property<T>;
};

export class Select<T> extends LabelledControl {
    readonly element = el.div({ class: "core_Select" });

    readonly preferred_label_position: LabelPosition;

    readonly selected: WritableProperty<T | undefined>;

    private readonly to_label: (element: T) => string;
    private readonly button: Button;
    private readonly menu: Menu<T>;
    private readonly _selected: WidgetProperty<T | undefined>;
    private just_opened: boolean;

    constructor(
        items: readonly T[] | Property<readonly T[]>,
        to_label: (element: T) => string,
        options?: SelectOptions<T>,
    ) {
        super(options);

        this.preferred_label_position = "left";

        this.to_label = to_label;
        this.button = this.disposable(
            new Button(" ", {
                icon_right: Icon.TriangleDown,
            }),
        );
        this.menu = this.disposable(new Menu<T>(items, to_label, this.element));
        this.element.append(this.button.element, this.menu.element);

        this._selected = new WidgetProperty<T | undefined>(this, undefined, this.set_selected);
        this.selected = this._selected;

        this.just_opened = false;

        this.disposables(
            disposable_listener(this.button.element, "mousedown", e => this.button_mousedown(e)),

            this.button.mouseup.observe(() => this.button_mouseup()),

            this.menu.selected.observe(({ value }) =>
                this._selected.set_val(value, { silent: false }),
            ),
        );

        if (options) {
            if (is_any_property(options.selected)) {
                this.selected.bind_to(options.selected);
            } else if (options.selected) {
                this.selected.val = options.selected;
            }
        }

        this.finalize_construction(Select.prototype);
    }

    protected set_enabled(enabled: boolean): void {
        super.set_enabled(enabled);
        this.button.enabled.val = enabled;
    }

    protected set_selected(selected?: T): void {
        this.button.text.val = selected ? this.to_label(selected) : " ";
        this.menu.selected.val = selected;
    }

    private button_mousedown(e: Event): void {
        e.stopPropagation();
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
