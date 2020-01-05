import { Disposable } from "../observable/Disposable";
import { Observable } from "../observable/Observable";
import { is_property } from "../observable/property/Property";
import { SectionId } from "../model";
import {
    ListChange,
    ListChangeEvent,
    ListChangeType,
    ListProperty,
} from "../observable/property/list/ListProperty";
import { Disposer } from "../observable/Disposer";
import { LogManager } from "../Logger";

const logger = LogManager.get("core/gui/dom");

export type Attributes<E> = Partial<E> & { data?: { [key: string]: string } };

export type Child = string | Node;

export function a(
    attributes?: Attributes<HTMLAnchorElement>,
    ...children: Child[]
): HTMLAnchorElement {
    const element = create_element("a", attributes, ...children);

    if (attributes && attributes.href && attributes.href.trimLeft().startsWith("http")) {
        element.target = "_blank";
        element.rel = "noopener noreferrer";
    }

    return element;
}

export function button(
    attributes?: Attributes<HTMLButtonElement>,
    ...children: Child[]
): HTMLButtonElement {
    return create_element("button", attributes, ...children);
}

export function div(attributes?: Attributes<HTMLDivElement>, ...children: Child[]): HTMLDivElement {
    return create_element("div", attributes, ...children);
}

export function h2(
    attributes?: Attributes<HTMLHeadingElement>,
    ...children: Child[]
): HTMLHeadingElement {
    return create_element("h2", attributes, ...children);
}

export function input(
    attributes?: Attributes<HTMLInputElement>,
    ...children: HTMLImageElement[]
): HTMLInputElement {
    return create_element("input", attributes, ...children);
}

export function img(
    attributes?: Attributes<HTMLImageElement>,
    ...children: HTMLImageElement[]
): HTMLImageElement {
    return create_element("img", attributes, ...children);
}

export function label(
    attributes?: Attributes<HTMLLabelElement>,
    ...children: Child[]
): HTMLLabelElement {
    return create_element("label", attributes, ...children);
}

export function li(attributes?: Attributes<HTMLLIElement>, ...children: Child[]): HTMLLIElement {
    return create_element("li", attributes, ...children);
}

export function p(
    attributes?: Attributes<HTMLParagraphElement>,
    ...children: Child[]
): HTMLParagraphElement {
    return create_element("p", attributes, ...children);
}

export function span(
    attributes?: Attributes<HTMLSpanElement>,
    ...children: Child[]
): HTMLSpanElement {
    return create_element("span", attributes, ...children);
}

export function table(
    attributes?: Attributes<HTMLTableElement>,
    ...children: Child[]
): HTMLTableElement {
    return create_element("table", attributes, ...children);
}

export function tbody(
    attributes?: Attributes<HTMLTableSectionElement>,
    ...children: Child[]
): HTMLTableSectionElement {
    return create_element("tbody", attributes, ...children);
}

export function td(
    attributes?: Attributes<HTMLTableCellElement>,
    ...children: Child[]
): HTMLTableCellElement {
    return create_element("td", attributes, ...children);
}

export function textarea(
    attributes?: Attributes<HTMLTextAreaElement>,
    ...children: Child[]
): HTMLTextAreaElement {
    return create_element("textarea", attributes, ...children);
}

export function tfoot(
    attributes?: Attributes<HTMLTableSectionElement>,
    ...children: Child[]
): HTMLTableSectionElement {
    return create_element("tfoot", attributes, ...children);
}

export function th(
    attributes?: Attributes<HTMLTableHeaderCellElement>,
    ...children: Child[]
): HTMLTableHeaderCellElement {
    return create_element("th", attributes, ...children);
}

export function thead(
    attributes?: Attributes<HTMLTableSectionElement>,
    ...children: Child[]
): HTMLTableSectionElement {
    return create_element("thead", attributes, ...children);
}

export function tr(
    attributes?: Attributes<HTMLTableRowElement>,
    ...children: Child[]
): HTMLTableRowElement {
    return create_element("tr", attributes, ...children);
}

export function ul(
    attributes?: Attributes<HTMLUListElement>,
    ...children: Child[]
): HTMLUListElement {
    return create_element("ul", attributes, ...children);
}

function create_element<E extends HTMLElement>(
    tag_name: string,
    attributes?: Attributes<E>,
    ...children: Child[]
): E {
    const element = (document.createElement(tag_name) as any) as E;

    if (attributes) {
        // noinspection SuspiciousTypeOfGuard
        if (attributes instanceof Node || typeof attributes === "string") {
            element.append(attributes);
        } else {
            const data = attributes.data;
            delete attributes.data;
            Object.assign(element, attributes);

            if (data) {
                for (const [key, val] of Object.entries(data)) {
                    element.dataset[key] = val;
                }
            }
        }
    }

    element.append(...children);

    return element;
}

export function bind_attr<E extends Element, A extends keyof E>(
    element: E,
    attribute: A,
    observable: Observable<E[A]>,
): Disposable {
    if (is_property<E[A]>(observable)) {
        element[attribute] = observable.val;
    }

    return observable.observe(({ value }) => (element[attribute] = value));
}

export enum Icon {
    ArrowDown,
    Eye,
    File,
    GitHub,
    LevelDown,
    LevelUp,
    LongArrowRight,
    NewFile,
    Play,
    Plus,
    Redo,
    Remove,
    Save,
    SquareArrowRight,
    Stop,
    TriangleDown,
    TriangleUp,
    Undo,
}

export function icon(icon: Icon): HTMLElement {
    let icon_str!: string;

    switch (icon) {
        case Icon.ArrowDown:
            icon_str = "fas fa-arrow-down";
            break;
        case Icon.Eye:
            icon_str = "far fa-eye";
            break;
        case Icon.File:
            icon_str = "fas fa-file";
            break;
        case Icon.GitHub:
            icon_str = "fab fa-github";
            break;
        case Icon.LevelDown:
            icon_str = "fas fa-level-down-alt";
            break;
        case Icon.LevelUp:
            icon_str = "fas fa-level-up-alt";
            break;
        case Icon.LongArrowRight:
            icon_str = "fas fa-long-arrow-alt-right";
            break;
        case Icon.NewFile:
            icon_str = "fas fa-file-medical";
            break;
        case Icon.Play:
            icon_str = "fas fa-play";
            break;
        case Icon.Plus:
            icon_str = "fas fa-plus";
            break;
        case Icon.Redo:
            icon_str = "fas fa-redo";
            break;
        case Icon.Remove:
            icon_str = "fas fa-trash-alt";
            break;
        case Icon.Save:
            icon_str = "fas fa-save";
            break;
        case Icon.Stop:
            icon_str = "fas fa-stop";
            break;
        case Icon.SquareArrowRight:
            icon_str = "far fa-caret-square-right";
            break;
        case Icon.TriangleDown:
            icon_str = "fas fa-caret-down";
            break;
        case Icon.TriangleUp:
            icon_str = "fas fa-caret-up";
            break;
        case Icon.Undo:
            icon_str = "fas fa-undo";
            break;
    }

    // Wrap the span in another span, because Font Awesome will replace the inner element. This way
    // the returned element will stay valid.
    return span(span({ className: icon_str }));
}

export function section_id_icon(section_id: SectionId, options?: { size?: number }): HTMLElement {
    const element = span();
    const size = options && options.size;

    element.style.display = "inline-block";
    element.style.width = `${size}px`;
    element.style.height = `${size}px`;
    element.style.backgroundImage = `url(${process.env.PUBLIC_URL}/images/sectionids/${SectionId[section_id]}.png)`;
    element.style.backgroundSize = `${size}px`;
    element.title = SectionId[section_id];

    return element;
}

export function disposable_listener<K extends keyof GlobalEventHandlersEventMap>(
    target: GlobalEventHandlers,
    type: K,
    listener: (this: GlobalEventHandlers, ev: GlobalEventHandlersEventMap[K]) => any,
    options?: AddEventListenerOptions,
): Disposable;
export function disposable_listener<K extends keyof WindowEventHandlersEventMap>(
    target: WindowEventHandlers,
    type: K,
    listener: (this: WindowEventHandlers, ev: WindowEventHandlersEventMap[K]) => any,
    options?: AddEventListenerOptions,
): Disposable;
export function disposable_listener<K extends keyof DocumentAndElementEventHandlersEventMap>(
    target: DocumentAndElementEventHandlers,
    type: K,
    listener: (
        this: DocumentAndElementEventHandlers,
        ev: DocumentAndElementEventHandlersEventMap[K],
    ) => any,
    options?: AddEventListenerOptions,
): Disposable;
export function disposable_listener(
    target:
        | GlobalEventHandlers
        | DocumentAndElementEventHandlers
        | WindowEventHandlers
        | EventTarget,
    type: string,
    listener: EventListenerOrEventListenerObject,
    options?: AddEventListenerOptions,
): Disposable {
    target.addEventListener(type, listener, options);

    return {
        dispose(): void {
            target.removeEventListener(type, listener);
        },
    };
}

/**
 * More lax definition of {@link disposable_listener} for custom and experimental event types.
 */
export function disposable_custom_listener(
    target: EventTarget,
    type: string,
    listener: EventListenerOrEventListenerObject,
    options?: AddEventListenerOptions,
): Disposable {
    target.addEventListener(type, listener, options);

    return {
        dispose(): void {
            target.removeEventListener(type, listener);
        },
    };
}

export function bind_children_to<T>(
    element: Element,
    list: ListProperty<T>,
    create_child: (value: T, index: number) => Element | [Element, Disposable],
    options?: {
        after?: (change: ListChangeEvent<T>) => void;
    },
): Disposable {
    const children_disposer = new Disposer();

    const observer = list.observe_list(
        (change: ListChangeEvent<T>) => {
            if (change.type === ListChangeType.ListChange) {
                splice_children(change);
            } else if (change.type === ListChangeType.ValueChange) {
                // TODO: update children
            }

            options?.after?.(change);
        },
        { call_now: true },
    );

    function splice_children(change: ListChange<T>): void {
        for (let i = 0; i < change.removed.length; i++) {
            const child_element = element.children[change.index];

            if (child_element) {
                child_element.remove();
            } else {
                logger.warn(
                    `Expected an element for removal at child index ${
                        change.index
                    } of ${node_to_string(element)} (child count: ${element.childElementCount}).`,
                );
            }
        }

        children_disposer.dispose_at(change.index, change.removed.length);

        const children = change.inserted.map((value, i) => {
            const child = create_child(value, change.index + i);

            if (Array.isArray(child)) {
                children_disposer.insert(change.index + i, child[1]);
                return child[0];
            } else {
                return child;
            }
        });

        if (change.index >= element.childElementCount) {
            element.append(...children);
        } else {
            for (let i = 0; i < children.length; i++) {
                element.children[change.index + i].insertAdjacentElement(
                    "beforebegin",
                    children[i],
                );
            }
        }
    }

    return {
        dispose(): void {
            observer.dispose();
            children_disposer.dispose();
            element.innerHTML = "";
        },
    };
}

function node_to_string(node: Node): string {
    const str = ["<", node.nodeName.toLowerCase()];

    if (node instanceof Element) {
        if (node.id) {
            str.push(' id="', node.id, '"');
        }

        if (node.className) {
            str.push(' className="', node.className, '"');
        }
    }

    str.push("/>");
    return str.join("");
}
