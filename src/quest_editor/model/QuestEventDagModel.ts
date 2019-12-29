import { QuestEventModel } from "./QuestEventModel";
import { ListProperty } from "../../core/observable/property/list/ListProperty";
import { WritableListProperty } from "../../core/observable/property/list/WritableListProperty";
import { list_property } from "../../core/observable";
import { defined, require_array, require_non_negative_integer } from "../../core/util";

export type QuestEventDagMeta = {
    readonly parents: QuestEventModel[];
    readonly children: QuestEventModel[];
};

/**
 * Events can call each other and form a directed acyclic graph (DAG) this way.
 */
export class QuestEventDagModel {
    private readonly _events: WritableListProperty<QuestEventModel>;
    private readonly _root_events: WritableListProperty<QuestEventModel>;
    private meta: Map<QuestEventModel, QuestEventDagMeta>;

    readonly area_id: number;

    readonly events: ListProperty<QuestEventModel>;

    /**
     * The root nodes of the DAG. These events are not called from any other events.
     */
    readonly root_events: ListProperty<QuestEventModel>;

    constructor(
        area_id: number,
        events: QuestEventModel[],
        root_events: QuestEventModel[],
        meta: Map<QuestEventModel, QuestEventDagMeta>,
    ) {
        require_non_negative_integer(area_id, "area_id");
        require_array(events, "events");
        require_array(root_events, "root_events");
        defined(meta, "meta");

        this.area_id = area_id;
        this._events = list_property(undefined, ...events);
        this.events = this._events;
        this._root_events = list_property(undefined, ...root_events);
        this.root_events = this._root_events;
        this.meta = meta;
    }

    get_children(event: QuestEventModel): readonly QuestEventModel[] {
        return this.meta.get(event)?.children ?? [];
    }

    remove_event(event: QuestEventModel): void {
        const meta = this.meta.get(event);

        if (meta) {
            const children = meta.children.slice();

            for (const child of children) {
                this.remove_child(event, child);
            }

            if (meta.parents.length) {
                for (const parent of meta.parents) {
                    // Connect event's parents to its children.
                    for (const child of children) {
                        this.add_child(parent, child);
                    }

                    this.remove_parent(event, parent);
                }
            } else {
                this._root_events.remove(event);
            }

            this.meta.delete(event);
        }

        this._events.remove(event);
    }

    private remove_child(event: QuestEventModel, child: QuestEventModel): void {
        const child_meta = this.meta.get(child);

        if (child_meta) {
            const index = child_meta.parents.indexOf(event);

            if (index !== -1) {
                child_meta.parents.splice(index, 1);
            }

            if (child_meta.parents.length === 0) {
                this._root_events.push(child);
            }
        }
    }

    private remove_parent(event: QuestEventModel, parent: QuestEventModel): void {
        const parent_meta = this.meta.get(parent);

        if (parent_meta) {
            const index = parent_meta.children.indexOf(event);

            if (index !== -1) {
                parent_meta.children.splice(index, 1);
            }
        }
    }

    private add_child(event: QuestEventModel, child: QuestEventModel): void {
        const meta = this.meta.get(event);

        if (meta && !meta.children.includes(child)) {
            meta.children.push(child);
        }

        const child_meta = this.meta.get(child);

        if (child_meta && !child_meta.parents.includes(event)) {
            child_meta.parents.push(event);
            this._root_events.remove(child);
        }
    }
}
