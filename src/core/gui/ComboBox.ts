import { LabelledControl, LabelledControlOptions } from "./LabelledControl";
import { create_element, el, Icon, icon } from "./dom";
import "./ComboBox.css";
import "./Input.css";
import { Menu } from "./Menu";
import { Property } from "../observable/property/Property";
import { property } from "../observable";
import { WritableProperty } from "../observable/property/WritableProperty";
import { WidgetProperty } from "../observable/property/WidgetProperty";

export type ComboBoxOptions<T> = LabelledControlOptions & {
    items: T[] | Property<T[]>;
    to_label(item: T): string;
    placeholder_text?: string;
    filter?(text: string): void;
};

export class ComboBox<T> extends LabelledControl {
    readonly preferred_label_position = "left";

    readonly selected: WritableProperty<T | undefined>;

    private readonly to_label: (element: T) => string;
    private readonly menu: Menu<T>;
    private readonly input_element: HTMLInputElement = create_element("input");
    private readonly _selected: WidgetProperty<T | undefined>;

    constructor(options: ComboBoxOptions<T>) {
        super(el.span({ class: "core_ComboBox core_Input" }), options);

        this.to_label = options.to_label;

        this._selected = new WidgetProperty<T | undefined>(this, undefined, this.set_selected);
        this.selected = this._selected;

        const menu_visible = property(false);

        this.menu = this.disposable(new Menu(options.items, options.to_label, this.element));
        this.menu.element.onmousedown = e => e.preventDefault();

        this.input_element.placeholder = options.placeholder_text || "";
        this.input_element.onmousedown = () => {
            menu_visible.val = true;
        };

        this.input_element.onkeydown = (e: Event) => {
            const key = (e as KeyboardEvent).key;

            switch (key) {
                case "ArrowDown":
                    e.preventDefault();
                    this.menu.hover_next();
                    break;

                case "ArrowUp":
                    e.preventDefault();
                    this.menu.hover_prev();
                    break;

                case "Enter":
                    this.menu.select_hovered();
                    break;
            }
        };

        const filter = options.filter;

        if (filter) {
            let input_value = "";

            this.input_element.onkeyup = () => {
                if (this.input_element.value !== input_value) {
                    input_value = this.input_element.value;
                    filter(input_value);

                    if (this.menu.visible.val || input_value) {
                        this.menu.hover_next();
                    }
                }
            };
        }

        this.input_element.onblur = () => {
            menu_visible.val = false;
        };

        const down_arrow_element = el.span({}, icon(Icon.TriangleDown));
        this.bind_hidden(down_arrow_element, menu_visible);

        const up_arrow_element = el.span({}, icon(Icon.TriangleUp));
        this.bind_hidden(up_arrow_element, menu_visible.map(v => !v));

        const button_element = el.span(
            { class: "core_ComboBox_button" },
            down_arrow_element,
            up_arrow_element,
        );
        button_element.onmousedown = e => {
            e.preventDefault();
            menu_visible.val = !menu_visible.val;
        };

        this.element.append(
            el.span(
                { class: "core_ComboBox_inner core_Input_inner" },
                this.input_element,
                button_element,
            ),
            this.menu.element,
        );

        this.disposables(
            this.menu.visible.bind_bi(menu_visible),

            menu_visible.observe(({ value: visible }) => {
                if (visible) {
                    this.menu.hover_next();
                }
            }),

            this.menu.selected.observe(({ value }) => {
                this.selected.set_val(value, { silent: false });
                this.input_element.focus();
            }),
        );
    }

    protected set_selected(selected?: T): void {
        this.input_element.value = selected ? this.to_label(selected) : "";
        this.menu.selected.val = selected;
    }
}
