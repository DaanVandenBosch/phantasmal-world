import { ResizableWidget } from "../../core/gui/ResizableWidget";
import { bind_children_to, el } from "../../core/gui/dom";
import "./EntityListView.css";
import { entity_data, EntityType } from "../../core/data_formats/parsing/quest/entities";
import { ListProperty } from "../../core/observable/property/list/ListProperty";
import { entity_dnd_source } from "./entity_dnd";
import { render_entity_to_image } from "../rendering/render_entity_to_image";

export abstract class EntityListView<T extends EntityType> extends ResizableWidget {
    readonly element: HTMLElement;

    protected constructor(private readonly class_name: string, entities: ListProperty<T>) {
        super();

        const list_element = el.div({ class: "quest_editor_EntityListView_entity_list" });

        this.element = el.div({ class: `${class_name} quest_editor_EntityListView` }, list_element);

        this.disposables(
            bind_children_to(list_element, entities, this.create_entity_element),

            entity_dnd_source(list_element, target => {
                let element: HTMLElement | null = target;

                do {
                    const index = target.dataset.index;

                    if (index != undefined) {
                        return [
                            element.querySelector("img")!.cloneNode(true) as HTMLElement,
                            entities.get(parseInt(index, 10)),
                        ];
                    }

                    element = element.parentElement;
                } while (element && element !== list_element);

                return undefined;
            }),
        );
    }

    private create_entity_element = (entity: T, index: number): HTMLElement => {
        const entity_element = el.div({
            class: "quest_editor_EntityListView_entity",
            data: { index: index.toString() },
        });
        entity_element.draggable = true;

        const img_element = el.img({ width: 100, height: 100 });
        img_element.style.visibility = "hidden";
        // Workaround for Chrome bug: when dragging an image, calling setDragImage on a DragEvent
        // has no effect.
        img_element.style.pointerEvents = "none";
        entity_element.append(img_element);

        render_entity_to_image(entity).then(url => {
            img_element.src = url;
            img_element.style.visibility = "visible";
        });

        const name_element = el.span({
            text: entity_data(entity).name,
        });
        entity_element.append(name_element);

        return entity_element;
    };
}
