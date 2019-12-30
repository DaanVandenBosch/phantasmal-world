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

    get_parents(event: QuestEventModel): readonly QuestEventModel[] {
        return this.meta.get(event)?.parents ?? [];
    }

    add_event_at(
        index: number,
        event: QuestEventModel,
        children: readonly QuestEventModel[],
        parents: readonly QuestEventModel[],
    ): void {
        this.remove_event(event);

        this.meta.set(event, { children: [], parents: [] });

        if (children.length === 0) {
            this._root_events.push(event);
        } else {
            for (const child of children) {
                this.add_edge(event, child);
            }
        }

        for (const parent of parents) {
            this.add_edge(parent, event);
        }

        this._events.splice(index, 0, event);
    }

    remove_event(event: QuestEventModel): void {
        const meta = this.meta.get(event);

        if (meta) {
            // Remove the edges from the event to its children.
            while (meta.children.length) {
                this.remove_edge(event, meta.children[0]);
            }

            // Remove the edges from the event to its parents.
            if (meta.parents.length) {
                while (meta.parents.length) {
                    this.remove_edge(meta.parents[0], event);
                }
            } else {
                this._root_events.remove(event);
            }

            this.meta.delete(event);
            this._events.remove(event);
        }
    }

    /**
     * Add a parent-child relationship between two events.
     */
    add_edge(parent: QuestEventModel, child: QuestEventModel): void {
        const parent_meta = this.meta.get(parent);

        if (parent_meta && !parent_meta.children.includes(child)) {
            parent_meta.children.push(child);
        }

        const child_meta = this.meta.get(child);

        if (child_meta && !child_meta.parents.includes(parent)) {
            child_meta.parents.push(parent);
            this._root_events.remove(child);
        }
    }

    /**
     * Remove a parent-child relationship between two events.
     */
    remove_edge(parent: QuestEventModel, child: QuestEventModel): void {
        const parent_meta = this.meta.get(parent);

        if (parent_meta) {
            const index = parent_meta.children.indexOf(child);

            if (index !== -1) {
                parent_meta.children.splice(index, 1);
            }
        }

        const child_meta = this.meta.get(child);

        if (child_meta) {
            const index = child_meta.parents.indexOf(parent);

            if (index !== -1) {
                child_meta.parents.splice(index, 1);
            }

            if (child_meta.parents.length === 0) {
                this._root_events.push(child);
            }
        }
    }
}
