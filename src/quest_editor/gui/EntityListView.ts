import { ResizableWidget } from "../../core/gui/ResizableWidget";
import { bind_children_to, el } from "../../core/gui/dom";
import "./EntityListView.css";
import { entity_data, EntityType } from "../../core/data_formats/parsing/quest/entities";
import { ListProperty } from "../../core/observable/property/list/ListProperty";
import { entity_dnd_source } from "./entity_dnd";

export abstract class EntityListView<T extends EntityType> extends ResizableWidget {
    readonly element: HTMLElement;

    protected constructor(private readonly class_name: string, entities: ListProperty<T>) {
        super();

        const list_element = el.div({ class: "quest_editor_EntityListView_entity_list" });

        this.element = el.div({ class: `${class_name} quest_editor_EntityListView` }, list_element);

        this.disposables(
            bind_children_to(list_element, entities, this.create_entity_element),

            entity_dnd_source(list_element, target => {
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
