import { LabelledControl, LabelledControlOptions, LabelPosition } from "./LabelledControl";
import { disposable_listener, el, Icon } from "./dom";
import "./Select.css";
import "./Button.css";
import { is_any_property, Property } from "../observable/property/Property";
import { Button } from "./Button";
import { WritableProperty } from "../observable/property/WritableProperty";
import { WidgetProperty } from "../observable/property/WidgetProperty";

export type SelectOptions<T> = LabelledControlOptions & {
    selected: T | Property<T>;
};

export class Select<T> extends LabelledControl {
    readonly preferred_label_position: LabelPosition;

    readonly selected: WritableProperty<T | undefined>;

    private readonly to_label: (element: T) => string;
    private readonly button: Button;
    private readonly element_container: HTMLElement;
    private readonly elements: Property<T[]>;
    private readonly _selected: WidgetProperty<T | undefined>;
    private just_opened: boolean;

    constructor(
        elements: Property<T[]>,
        to_label: (element: T) => string,
        options?: SelectOptions<T>,
    ) {
        const button = new Button("", {
            icon_right: Icon.TriangleDown,
        });

        const element_container = el.div({ class: "core_Select_elements" });

        super(el.div({ class: "core_Select" }, button.element, element_container), options);

        this.element_container = element_container;
        this.element_container.hidden = true;
        this.element_container.onmouseup = (e: Event) => this.element_container_mouseup(e);

        const element_container_inner = el.div({ class: "core_Select_elements_inner" });
        element_container.append(element_container_inner);

        this.preferred_label_position = "left";

        this.to_label = to_label;

        this.button = this.disposable(button);

        this.elements = elements;

        this._selected = new WidgetProperty<T | undefined>(this, undefined, this.set_selected);
        this.selected = this._selected;

        this.just_opened = false;

        this.disposables(
            elements.observe(
                ({ value: opts }) => {
                    element_container_inner.innerHTML = "";
                    element_container_inner.append(
                        ...opts.map((opt, index) =>
                            el.div({ text: to_label(opt), data: { index: index.toString() } }),
                        ),
                    );
                },
                { call_now: true },
            ),

            button.mousedown.observe(() => this.button_mousedown()),

            button.mouseup.observe(() => this.button_mouseup()),

            disposable_listener(document, "mousedown", (e: Event) => this.document_mousedown(e)),
        );

        if (options) {
            if (is_any_property(options.selected)) {
                this.selected.bind_to(options.selected);
            } else if (options.selected) {
                this.selected.val = options.selected;
            }
        }
    }

    protected set_enabled(enabled: boolean): void {
        super.set_enabled(enabled);
        this.button.enabled.val = enabled;
    }

    protected set_selected(selected?: T): void {
        this.button.text.val = selected ? this.to_label(selected) : "";
    }

    private button_mousedown(): void {
        this.just_opened = this.element_container.hidden;
        this.show_menu();
    }

    private button_mouseup(): void {
        if (!this.just_opened) {
            this.hide_menu();
        }

        this.just_opened = false;
    }

    private element_container_mouseup(e: Event): void {
        if (!(e.target instanceof HTMLElement)) return;

        const index_str = e.target.dataset.index;
        if (index_str == undefined) return;

        const element = this.elements.val[parseInt(index_str, 10)];
        if (!element) return;

        this.selected.set_val(element, { silent: false });
        this.hide_menu();
    }

    private document_mousedown(e: Event): void {
        if (!this.element.contains(e.target as Node)) {
            this.hide_menu();
        }
    }

    private show_menu(): void {
        this.element_container.hidden = false;
    }

    private hide_menu(): void {
        this.element_container.hidden = true;
    }
}
