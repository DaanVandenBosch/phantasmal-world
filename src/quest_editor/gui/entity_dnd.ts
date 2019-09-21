import { entity_data, EntityType } from "../../core/data_formats/parsing/quest/entities";
import { Disposable } from "../../core/observable/Disposable";
import { Vec2 } from "../../core/data_formats/vector";
import { el } from "../../core/gui/dom";

export type EntityDragEvent = {
    readonly entity_type: EntityType;
    readonly drag_element: HTMLElement;
    readonly event: DragEvent;
};

let dragging_details: Omit<EntityDragEvent, "event"> | undefined = undefined;
const listeners: Map<(e: EntityDragEvent) => void, (e: DragEvent) => void> = new Map();
const grab_point = new Vec2(0, 0);
let drag_sources = 0;

export function add_entity_dnd_listener(
    element: HTMLElement,
    type: "dragenter" | "dragover" | "dragleave" | "drop",
    listener: (event: EntityDragEvent) => void,
): void {
    function event_listener(event: DragEvent): void {
        if (dragging_details) {
            listener({ ...dragging_details, event });
        }
    }

    listeners.set(listener, event_listener);
    element.addEventListener(type, event_listener);
}

export function remove_entity_dnd_listener(
    element: HTMLElement,
    type: "dragenter" | "dragover" | "dragleave" | "drop",
    listener: (event: EntityDragEvent) => void,
): void {
    const event_listener = listeners.get(listener);

    if (event_listener) {
        listeners.delete(listener);
        element.removeEventListener(type, event_listener);
    }
}

export function entity_dnd_source(
    element: HTMLElement,
    start: (target: HTMLElement) => [HTMLElement, EntityType] | undefined,
): Disposable {
    function dragstart(e: DragEvent): void {
        if (e.target instanceof HTMLElement) {
            const result = start(e.target);

            if (result) {
                grab_point.set(e.offsetX, e.offsetY);

                dragging_details = {
                    drag_element: result[0],
                    entity_type: result[1],
                };

                dragging_details.drag_element.style.position = "fixed";
                dragging_details.drag_element.style.pointerEvents = "none";
                dragging_details.drag_element.style.zIndex = "500";
                dragging_details.drag_element.style.top = "0";
                dragging_details.drag_element.style.left = "0";
                dragging_details.drag_element.style.transform = `translate(${e.clientX -
                    grab_point.x}px, ${e.clientY - grab_point.y}px)`;
                document.body.append(dragging_details.drag_element);

                if (e.dataTransfer) {
                    e.dataTransfer.effectAllowed = "copy";
                    e.dataTransfer.setDragImage(el.div(), 0, 0);
                    // setData is necessary for FireFox.
                    e.dataTransfer.setData(
                        "phantasmal-entity",
                        entity_data(dragging_details.entity_type).name,
                    );
                }
            } else {
                e.preventDefault();
            }
        }
    }

    element.addEventListener("dragstart", dragstart);

    if (++drag_sources === 1) {
        document.addEventListener("dragover", dragover);
        document.addEventListener("dragend", dragend);
    }

    return {
        dispose(): void {
            element.removeEventListener("dragstart", dragstart);

            if (--drag_sources === 0) {
                document.removeEventListener("dragover", dragover);
                document.removeEventListener("dragend", dragend);
            }
        },
    };
}

function dragover(e: DragEvent): void {
    e.preventDefault();

    if (e.dataTransfer) {
        e.dataTransfer.dropEffect = "none";
    }

    if (dragging_details) {
        dragging_details.drag_element.style.transform = `translate(${e.clientX -
            grab_point.x}px, ${e.clientY - grab_point.y}px)`;
    }
}

function dragend(): void {
    if (dragging_details) {
        dragging_details.drag_element.remove();
        dragging_details = undefined;
    }
}
