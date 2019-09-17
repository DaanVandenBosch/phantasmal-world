import { Disposable } from "../observable/Disposable";
import { Observable } from "../observable/Observable";
import { is_property } from "../observable/property/Property";
import { SectionId } from "../model";
import {
    ListChangeType,
    ListProperty,
    ListPropertyChangeEvent,
} from "../observable/property/list/ListProperty";
import { Disposer } from "../observable/Disposer";

type ElementAttributes = {
    class?: string;
    tab_index?: number;
    text?: string;
    title?: string;
    data?: { [key: string]: string };
};

export const el = {
    div: (attributes?: ElementAttributes, ...children: HTMLElement[]): HTMLDivElement =>
        create_element("div", attributes, ...children),

    span: (attributes?: ElementAttributes, ...children: HTMLElement[]): HTMLSpanElement =>
        create_element("span", attributes, ...children),

    h2: (attributes?: ElementAttributes, ...children: HTMLElement[]): HTMLHeadingElement =>
        create_element("h2", attributes, ...children),

    p: (attributes?: ElementAttributes, ...children: HTMLElement[]): HTMLParagraphElement =>
        create_element("p", attributes, ...children),

    a: (
        attributes?: ElementAttributes & {
            href?: string;
        },
        ...children: HTMLElement[]
    ): HTMLAnchorElement => {
        const element = create_element<HTMLAnchorElement>("a", attributes, ...children);

        if (attributes && attributes.href && attributes.href.trimLeft().startsWith("http")) {
            element.target = "_blank";
            element.rel = "noopener noreferrer";
        }

        return element;
    },

    table: (attributes?: ElementAttributes, ...children: HTMLElement[]): HTMLTableElement =>
        create_element("table", attributes, ...children),

    thead: (attributes?: ElementAttributes, ...children: HTMLElement[]): HTMLTableSectionElement =>
        create_element("thead", attributes, ...children),

    tbody: (attributes?: ElementAttributes, ...children: HTMLElement[]): HTMLTableSectionElement =>
        create_element("tbody", attributes, ...children),

    tfoot: (attributes?: ElementAttributes, ...children: HTMLElement[]): HTMLTableSectionElement =>
        create_element("tfoot", attributes, ...children),

    tr: (attributes?: ElementAttributes, ...children: HTMLElement[]): HTMLTableRowElement =>
        create_element("tr", attributes, ...children),

    th: (
        attributes?: ElementAttributes & { col_span?: number },
        ...children: HTMLElement[]
    ): HTMLTableHeaderCellElement => create_element("th", attributes, ...children),

    td: (
        attributes?: ElementAttributes & { col_span?: number },
        ...children: HTMLElement[]
    ): HTMLTableCellElement => create_element("td", attributes, ...children),

    button: (attributes?: ElementAttributes, ...children: HTMLElement[]): HTMLButtonElement =>
        create_element("button", attributes, ...children),

    textarea: (attributes?: ElementAttributes, ...children: HTMLElement[]): HTMLTextAreaElement =>
        create_element("textarea", attributes, ...children),
};

export function create_element<T extends HTMLElement>(
    tag_name: string,
    attributes?: ElementAttributes & {
        href?: string;
        col_span?: number;
    },
    ...children: HTMLElement[]
): T {
    const element = document.createElement(tag_name) as (HTMLTableCellElement & HTMLAnchorElement);

    if (attributes) {
        if (attributes.class) element.className = attributes.class;
        if (attributes.text) element.textContent = attributes.text;
        if (attributes.title) element.title = attributes.title;
        if (attributes.href) element.href = attributes.href;

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
    GitHub,
}

export function icon(icon: Icon): HTMLElement {
    let icon_str!: string;

    switch (icon) {
        case Icon.File:
            icon_str = "fas fa-file";
            break;
        case Icon.NewFile:
            icon_str = "fas fa-file-medical";
            break;
        case Icon.Save:
            icon_str = "fas fa-save";
            break;
        case Icon.TriangleUp:
            icon_str = "fas fa-caret-up";
            break;
        case Icon.TriangleDown:
            icon_str = "fas fa-caret-down";
            break;
        case Icon.Undo:
            icon_str = "fas fa-undo";
            break;
        case Icon.Redo:
            icon_str = "fas fa-redo";
            break;
        case Icon.Remove:
            icon_str = "fas fa-trash-alt";
            break;
        case Icon.GitHub:
            icon_str = "fab fa-github";
            break;
    }

    return el.span({ class: icon_str });
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

export function bind_children_to<T>(
    element: HTMLElement,
    list: ListProperty<T>,
    create_child: (value: T, index: number) => HTMLElement | [HTMLElement, Disposable],
): Disposable {
    const children_disposer = new Disposer();

    const observer = list.observe_list((change: ListPropertyChangeEvent<T>) => {
        if (change.type === ListChangeType.ListChange) {
            splice_children(change.index, change.removed.length, change.inserted);
        } else if (change.type === ListChangeType.ValueChange) {
            // TODO: update children
        }
    });

    function splice_children(index: number, removed_count: number, inserted: T[]): void {
        for (let i = 0; i < removed_count; i++) {
            element.children[index].remove();
        }

        children_disposer.dispose_at(index, removed_count);

        const children = inserted.map((value, i) => {
            const child = create_child(value, index + i);

            if (Array.isArray(child)) {
                children_disposer.insert(index + i, child[1]);
                return child[0];
            } else {
                return child;
            }
        });

        if (index >= element.childElementCount) {
            element.append(...children);
        } else {
            for (let i = 0; i < removed_count; i++) {
                element.children[index + i].insertAdjacentElement("beforebegin", children[i]);
            }
        }
    }

    splice_children(0, 0, list.val);

    return {
        dispose(): void {
            observer.dispose();
            children_disposer.dispose();
            element.innerHTML = "";
        },
    };
}
