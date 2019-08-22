import { Disposable } from "../observable/Disposable";
import { Observable } from "../observable/Observable";
import { is_property } from "../observable/Property";

export function el<T extends HTMLElement>(
    tag_name: string,
    attributes?: {
        class?: string;
        text?: string ;
        data?: { [key: string]: string };
    },
    ...children: HTMLElement[]
): T {
    const element = document.createElement(tag_name) as T;

    if (attributes) {
        if (attributes.class) element.className = attributes.class;
        if (attributes.text) element.textContent = attributes.text;

        if (attributes.data) {
            for (const [key, val] of Object.entries(attributes.data)) {
                element.dataset[key] = val;
            }
        }
    }

    element.append(...children);

    return element;
}

export function bind_hidden(element: HTMLElement, observable: Observable<boolean>): Disposable {
    if (is_property(observable)) {
        element.hidden = observable.val;
    }

    return observable.observe(v => (element.hidden = v));
}
