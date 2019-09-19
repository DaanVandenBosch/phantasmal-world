import { ResizableWidget } from "../../core/gui/ResizableWidget";
import { bind_children_to, el } from "../../core/gui/dom";
import "./EntityListView.css";
import { entity_data, EntityType } from "../../core/data_formats/parsing/quest/entities";
import { ListProperty } from "../../core/observable/property/list/ListProperty";
import { Vec2 } from "../../core/data_formats/vector";
import { Disposable } from "../../core/observable/Disposable";

export abstract class EntityListView<T extends EntityType> extends ResizableWidget {
    readonly element: HTMLElement;

    protected constructor(private readonly class_name: string, entities: ListProperty<T>) {
        super();

        const list_element = el.div({ class: "quest_editor_EntityListView_entity_list" });

        this.element = el.div({ class: `${class_name} quest_editor_EntityListView` }, list_element);

        this.disposables(
            bind_children_to(list_element, entities, this.create_entity_element),

            make_draggable(list_element, target => {
                if (target !== list_element) {
                    const drag_element = target.cloneNode(true) as HTMLElement;
                    drag_element.style.width = "100px";
                    return [drag_element, entities.get(parseInt(target.dataset.index!, 10))];
                } else {
                    return undefined;
                }
            }),
        );
    }

    private create_entity_element = (entity: T, index: number): HTMLElement => {
        const div = el.div({
            class: "quest_editor_EntityListView_entity",
            text: entity_data(entity).name,
            data: { index: index.toString() },
        });

        div.draggable = true;

        return div;
    };
}

export type EntityDrag = {
    readonly offset_x: number;
    readonly offset_y: number;
    readonly data_transfer: DataTransfer;
    readonly drag_element: HTMLElement;
    readonly entity_type: EntityType;
};

function make_draggable(
    element: HTMLElement,
    start: (target: HTMLElement) => [HTMLElement, any] | undefined,
): Disposable {
    let detail: { drag_element: HTMLElement; entity_type: EntityType } | undefined;
    const grab_point = new Vec2(0, 0);

    function clear(): void {
        if (detail) {
            detail.drag_element.remove();
            detail = undefined;
        }
    }

    function redispatch(e: DragEvent): void {
        if (e.target instanceof HTMLElement && detail && e.dataTransfer) {
            e.target.dispatchEvent(
                new CustomEvent<EntityDrag>(`phantasmal-${e.type}`, {
                    detail: {
                        ...detail,
                        data_transfer: e.dataTransfer,
                        offset_x: e.offsetX,
                        offset_y: e.offsetY,
                    },
                    bubbles: true,
                }),
            );
        }
    }

    function dragstart(e: DragEvent): void {
        if (e.target instanceof HTMLElement) {
            clear();

            const result = start(e.target);

            if (result) {
                grab_point.set(e.offsetX + 2, e.offsetY + 2);

                detail = {
                    drag_element: result[0],
                    entity_type: result[1],
                };

                detail.drag_element.style.position = "fixed";
                detail.drag_element.style.pointerEvents = "none";
                detail.drag_element.style.zIndex = "500";
                detail.drag_element.style.top = "0";
                detail.drag_element.style.left = "0";
                detail.drag_element.style.transform = `translate(${e.clientX -
                    grab_point.x}px, ${e.clientY - grab_point.y}px)`;
                document.body.append(detail.drag_element);

                e.dataTransfer!.setDragImage(el.div(), 0, 0);
            }
        }
    }

    function dragenter(e: DragEvent): void {
        redispatch(e);
    }

    function dragover(e: DragEvent): void {
        if (e.target instanceof HTMLElement && detail) {
            detail.drag_element.style.transform = `translate(${e.clientX -
                grab_point.x}px, ${e.clientY - grab_point.y}px)`;

            redispatch(e);
        }
    }

    function dragleave(e: DragEvent): void {
        redispatch(e);
    }

    function dragend(): void {
        clear();
    }

    function drop(e: DragEvent): void {
        try {
            redispatch(e);
        } finally {
            clear();
        }
    }

    element.addEventListener("dragstart", dragstart);
    document.addEventListener("dragenter", dragenter);
    document.addEventListener("dragover", dragover);
    document.addEventListener("dragleave", dragleave);
    document.addEventListener("dragend", dragend);
    document.addEventListener("drop", drop);

    return {
        dispose(): void {
            element.removeEventListener("dragstart", dragstart);
            document.removeEventListener("dragenter", dragenter);
            document.removeEventListener("dragover", dragover);
            document.removeEventListener("dragleave", dragleave);
            document.removeEventListener("dragend", dragend);
            document.removeEventListener("drop", drop);

            clear();
        },
    };
}
