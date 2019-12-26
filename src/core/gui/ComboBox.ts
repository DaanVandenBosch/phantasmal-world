import { LabelledControl, LabelledControlOptions } from "./LabelledControl";
import { Icon, icon, input, span } from "./dom";
import "./ComboBox.css";
import "./Input.css";
import { Menu } from "./Menu";
import { Property } from "../observable/property/Property";
import { WritableProperty } from "../observable/property/WritableProperty";
import { WidgetProperty } from "../observable/property/WidgetProperty";

export type ComboBoxOptions<T> = LabelledControlOptions & {
    items: readonly T[] | Property<readonly T[]>;
    to_label(item: T): string;
    placeholder_text?: string;
    filter?(text: string): void;
};

export class ComboBox<T> extends LabelledControl {
    readonly element = span({ className: "core_ComboBox core_Input" });

    readonly preferred_label_position = "left";

    readonly selected: WritableProperty<T | undefined>;

    private readonly to_label: (element: T) => string;
    private readonly menu: Menu<T>;
    private readonly input_element: HTMLInputElement = input();
    private readonly _selected: WidgetProperty<T | undefined>;

    constructor(options: ComboBoxOptions<T>) {
        super(options);

        this.to_label = options.to_label;

        this._selected = new WidgetProperty<T | undefined>(this, undefined, this.set_selected);
        this.selected = this._selected;

        this.menu = this.disposable(
            new Menu({
                items: options.items,
                to_label: options.to_label,
                related_element: this.element,
            }),
        );
        this.menu.element.onmousedown = e => e.preventDefault();

        this.input_element.placeholder = options.placeholder_text || "";
        this.input_element.onmousedown = () => {
            this.menu.visible.set_val(true, { silent: false });
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
            this.menu.visible.set_val(false, { silent: false });
        };

        const down_arrow_element = span(icon(Icon.TriangleDown));
        this.bind_hidden(down_arrow_element, this.menu.visible);

        const up_arrow_element = span(icon(Icon.TriangleUp));
        this.bind_hidden(
            up_arrow_element,
            this.menu.visible.map(v => !v),
        );

        const button_element = span(
            { className: "core_ComboBox_button" },
            down_arrow_element,
            up_arrow_element,
        );
        button_element.onmousedown = e => {
            e.preventDefault();
            this.menu.visible.set_val(!this.menu.visible.val, { silent: false });
        };

        this.element.append(
            span(
                { className: "core_ComboBox_inner core_Input_inner" },
                this.input_element,
                button_element,
            ),
            this.menu.element,
        );

        this.disposables(
            this.menu.visible.observe(({ value: visible }) => {
                if (visible) {
                    this.menu.hover_next();
                }
            }),

            this.menu.selected.observe(({ value }) => {
                this.selected.set_val(value, { silent: false });
                this.input_element.focus();
            }),
        );

        this.finalize_construction();
    }

    protected set_selected(selected?: T): void {
        this.input_element.value = selected ? this.to_label(selected) : "";
        this.menu.selected.val = selected;
    }
}
