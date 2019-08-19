import { Disposable } from "./Disposable";

export function create_el<T extends HTMLElement>(
    tag_name: string,
    class_name?: string,
    modify?: (element: T) => void,
): T {
    const element = document.createElement(tag_name) as T;
    if (class_name) element.className = class_name;
    if (modify) modify(element);
    return element;
}

export function disposable_el(element: HTMLElement): Disposable {
    return {
        dispose(): void {
            element.remove();
        },
    };
}
