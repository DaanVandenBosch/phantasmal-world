import { Disposable } from "../observable/Disposable";
import { Observable } from "../observable/Observable";
import { is_property } from "../observable/property/Property";
import { SectionId } from "../model";

export const el = {
    div: (
        attributes?: {
            class?: string;
            tab_index?: number;
            text?: string;
            data?: { [key: string]: string };
        },
        ...children: HTMLElement[]
    ): HTMLDivElement => create_element("div", attributes, ...children),

    span: (
        attributes?: {
            class?: string;
            tab_index?: number;
            text?: string;
            data?: { [key: string]: string };
        },
        ...children: HTMLElement[]
    ): HTMLSpanElement => create_element("span", attributes, ...children),

    h2: (
        attributes?: {
            class?: string;
            tab_index?: number;
            text?: string;
            data?: { [key: string]: string };
        },
        ...children: HTMLElement[]
    ): HTMLHeadingElement => create_element("h2", attributes, ...children),

    table: (attributes?: {}, ...children: HTMLElement[]): HTMLTableElement =>
        create_element("table", attributes, ...children),

    thead: (attributes?: {}, ...children: HTMLElement[]): HTMLTableSectionElement =>
        create_element("thead", attributes, ...children),

    tbody: (attributes?: {}, ...children: HTMLElement[]): HTMLTableSectionElement =>
        create_element("tbody", attributes, ...children),

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

    button: (attributes?: {}, ...children: HTMLElement[]): HTMLButtonElement =>
        create_element("button", attributes, ...children),

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

export enum Icon {
    File,
    NewFile,
    Save,
    TriangleUp,
    TriangleDown,
    Undo,
    Redo,
    Remove,
}

export function icon(icon: Icon): HTMLElement {
    let icon_str!: string;

    switch (icon) {
        case Icon.File:
            icon_str = "fa-file";
            break;
        case Icon.NewFile:
            icon_str = "fa-file-medical";
            break;
        case Icon.Save:
            icon_str = "fa-save";
            break;
        case Icon.TriangleUp:
            icon_str = "fa-caret-up";
            break;
        case Icon.TriangleDown:
            icon_str = "fa-caret-down";
            break;
        case Icon.Undo:
            icon_str = "fa-undo";
            break;
        case Icon.Redo:
            icon_str = "fa-redo";
            break;
        case Icon.Remove:
            icon_str = "fa-trash-alt";
            break;
    }

    return el.span({ class: `fas ${icon_str}` });
}

export function section_id_icon(section_id: SectionId, options?: { size?: number }): HTMLElement {
    const element = el.span();
    const size = options && options.size;

    element.style.display = "inline-block";
    element.style.width = `${size}px`;
    element.style.height = `${size}px`;
    element.style.backgroundImage = `url(${process.env.PUBLIC_URL}/images/sectionids/${SectionId[section_id]}.png)`;
    element.style.backgroundSize = `${size}px`;
    element.title = SectionId[section_id];

    return element;
}

export function disposable_listener(
    element: DocumentAndElementEventHandlers,
    event: string,
    listener: EventListenerOrEventListenerObject,
    options?: AddEventListenerOptions,
): Disposable {
    element.addEventListener(event, listener, options);

    return {
        dispose(): void {
            element.removeEventListener(event, listener);
        },
    };
}
