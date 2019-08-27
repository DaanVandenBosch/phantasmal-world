import { Disposable } from "../observable/Disposable";
import { Observable } from "../observable/Observable";
import { is_property } from "../observable/Property";

export const el = {
    div: (
        attributes?: { class?: string; tab_index?: number },
        ...children: HTMLElement[]
    ): HTMLDivElement => create_element("div", attributes, ...children),

    table: (attributes?: {}, ...children: HTMLElement[]): HTMLTableElement =>
        create_element("table", attributes, ...children),

    tr: (attributes?: {}, ...children: HTMLElement[]): HTMLTableRowElement =>
        create_element("tr", attributes, ...children),

    th: (
        attributes?: { class?: string; text?: string; col_span?: number },
        ...children: HTMLElement[]
    ): HTMLTableHeaderCellElement => create_element("th", attributes, ...children),

    td: (
        attributes?: { text?: string; col_span?: number },
        ...children: HTMLElement[]
    ): HTMLTableCellElement => create_element("td", attributes, ...children),

    textarea: (attributes?: {}, ...children: HTMLElement[]): HTMLTextAreaElement =>
        create_element("textarea", attributes, ...children),
};

export function create_element<T extends HTMLElement>(
    tag_name: string,
    attributes?: {
        class?: string;
        tab_index?: number;
        text?: string;
        data?: { [key: string]: string };
        col_span?: number;
    },
    ...children: HTMLElement[]
): T {
    const element = document.createElement(tag_name) as HTMLTableCellElement;

    if (attributes) {
        if (attributes.class) element.className = attributes.class;
        if (attributes.text) element.textContent = attributes.text;

        if (attributes.data) {
            for (const [key, val] of Object.entries(attributes.data)) {
                element.dataset[key] = val;
            }
        }

        if (attributes.col_span) element.colSpan = attributes.col_span;

        if (attributes.tab_index) element.tabIndex = attributes.tab_index;
    }

    element.append(...children);

    return (element as HTMLElement) as T;
}

export function bind_hidden(element: HTMLElement, observable: Observable<boolean>): Disposable {
    if (is_property(observable)) {
        element.hidden = observable.val;
    }

    return observable.observe(({ value }) => (element.hidden = value));
}
