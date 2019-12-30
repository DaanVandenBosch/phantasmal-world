import { QuestEventModel } from "./QuestEventModel";
import { ListProperty } from "../../core/observable/property/list/ListProperty";
import { WritableListProperty } from "../../core/observable/property/list/WritableListProperty";
import { list_property } from "../../core/observable";
import { assert, defined, require_array, require_non_negative_integer } from "../../core/util";

export type QuestEventDagMeta = {
    readonly parents: QuestEventModel[];
    readonly children: QuestEventModel[];
};

/**
 * Events can call each other and form a directed acyclic graph (DAG) this way.
 * This DAG enforces connectivity.
 */
export class QuestEventDagModel {
    private readonly _events: WritableListProperty<QuestEventModel>;
    private meta: Map<QuestEventModel, QuestEventDagMeta>;

    readonly area_id: number;

    readonly events: ListProperty<QuestEventModel>;

    constructor(
        area_id: number,
        events: QuestEventModel[],
        meta: Map<QuestEventModel, QuestEventDagMeta>,
    ) {
        require_non_negative_integer(area_id, "area_id");
        require_array(events, "events");
        assert(
            meta.size === events.length,
            "meta and events should contain the same amount of elements",
        );
        defined(meta, "meta");

        this.area_id = area_id;
        this._events = list_property(undefined, ...events);
        this.events = this._events;
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
        // An event can only be added without parents if it will become the root event.
        if (children.length === 0) {
            assert(
                this.events.length.val === 0 || parents.length >= 1,
                "an event should have at least one parent",
            );
        } else {
            for (const child of children) {
                const child_meta = this.meta.get(child);

                if (child_meta && child_meta.parents.length > 0) {
                    assert(parents.length >= 1, "an event should have at least one parent");
                }
            }
        }

        this.remove_event(event);

        this.meta.set(event, { children: [], parents: [] });

        for (const child of children) {
            this.add_edge(event, child);
        }

        for (const parent of parents) {
            this.add_edge(parent, event);
        }

        this._events.splice(index, 0, event);
    }

    remove_event(event: QuestEventModel): void {
        const meta = this.meta.get(event);

        if (meta) {
            // If the event has no parents (i.e. is the root event) and one or less children, we can
            // proceed without verifying edges. Otherwise we need to ensure its children have at
            // least one other parent.
            if (meta.parents.length === 0) {
                assert(
                    meta.children.length <= 1,
                    "event's children should have at least one other parent before removing event",
                );
            } else {
                for (const child of meta.children) {
                    assert(
                        this.meta.get(child)!.parents.length > 1,
                        "event's children should have at least one other parent before removing event",
                    );
                }
            }

            // Remove the edges from the event to its children.
            while (meta.children.length) {
                this.internal_remove_edge(event, meta, meta.children[0], undefined);
            }

            // Remove the edges from the event to its parents.
            while (meta.parents.length) {
                this.internal_remove_edge(meta.parents[0], undefined, event, meta);
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
        }
    }

    /**
     * Remove a parent-child relationship between two events.
     */
    remove_edge(parent: QuestEventModel, child: QuestEventModel, force: boolean = false): void {
        const child_meta = this.meta.get(child);

        if (child_meta && !force) {
            const index = child_meta.parents.indexOf(parent);

            if (index !== -1) {
                assert(
                    child_meta.parents.length > 1,
                    "child should have at least one other parent before removing the relationship between parent and child",
                );
            }
        }

        this.internal_remove_edge(parent, undefined, child, child_meta);
    }

    private internal_remove_edge(
        parent: QuestEventModel,
        parent_meta: QuestEventDagMeta | undefined = this.meta.get(parent),
        child: QuestEventModel,
        child_meta: QuestEventDagMeta | undefined = this.meta.get(child),
    ): void {
        if (parent_meta) {
            const index = parent_meta.children.indexOf(child);

            if (index !== -1) {
                parent_meta.children.splice(index, 1);
            }
        }

        if (child_meta) {
            const index = child_meta.parents.indexOf(parent);

            if (index !== -1) {
                child_meta.parents.splice(index, 1);
            }
        }
    }
}
