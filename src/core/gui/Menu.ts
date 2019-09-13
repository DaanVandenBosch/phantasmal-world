import { disposable_listener, el } from "./dom";
import { Widget } from "./Widget";
import { Property } from "../observable/property/Property";
import { property } from "../observable";
import { WritableProperty } from "../observable/property/WritableProperty";
import { WidgetProperty } from "../observable/property/WidgetProperty";
import "./Menu.css";

export class Menu<T> extends Widget {
    readonly selected: WritableProperty<T | undefined>;

    private readonly to_label: (element: T) => string;
    private readonly items: Property<T[]>;
    private readonly related_element: HTMLElement;
    private readonly _selected: WidgetProperty<T | undefined>;

    constructor(
        items: T[] | Property<T[]>,
        to_label: (element: T) => string,
        related_element: HTMLElement,
    ) {
        super(el.div({ class: "core_Menu" }));

        this.visible.val = false;

        this.element.onmouseup = (e: Event) => this.mouseup(e);

        const inner_element = el.div({ class: "core_Menu_inner" });
        this.element.append(inner_element);

        this.to_label = to_label;
        this.items = Array.isArray(items) ? property(items) : items;
        this.related_element = related_element;

        this._selected = new WidgetProperty<T | undefined>(this, undefined, this.set_selected);
        this.selected = this._selected;

        this.disposables(
            this.items.observe(
                ({ value: items }) => {
                    inner_element.innerHTML = "";
                    inner_element.append(
                        ...items.map((item, index) =>
                            el.div({ text: to_label(item), data: { index: index.toString() } }),
                        ),
                    );
                },
                { call_now: true },
            ),

            disposable_listener(document, "mousedown", this.document_mousedown, {
                capture: true,
            }),

            disposable_listener(document, "keydown", this.document_keydown),
        );
    }

    protected set_selected(): void {
        // Noop
    }

    private mouseup(e: Event): void {
        if (!(e.target instanceof HTMLElement)) return;

        const index_str = e.target.dataset.index;
        if (index_str == undefined) return;

        const element = this.items.val[parseInt(index_str, 10)];
        if (!element) return;

        this.selected.set_val(element, { silent: false });
        this.visible.set_val(false, { silent: false });
    }

    private document_mousedown = (e: Event): void => {
        if (
            this.visible.val &&
            !this.element.contains(e.target as Node) &&
            !this.related_element.contains(e.target as Node)
        ) {
            this.visible.set_val(false, { silent: false });
        }
    };

    private document_keydown = (e: Event): void => {
        if ((e as KeyboardEvent).key === "Escape") {
            this.visible.set_val(false, { silent: false });
        }
    };
}
