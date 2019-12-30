import { QuestEventModel } from "./QuestEventModel";
import { defined, require_non_negative_integer } from "../../core/util";
import { ChangeEvent, Observable } from "../../core/observable/Observable";
import { Disposable } from "../../core/observable/Disposable";
import { emitter, list_property } from "../../core/observable";
import { Emitter } from "../../core/observable/Emitter";
import { ListProperty } from "../../core/observable/property/list/ListProperty";
import { WritableListProperty } from "../../core/observable/property/list/WritableListProperty";

export enum QuestEventDagModelChangeType {
    NodeAdded,
    NodeRemoved,
    EdgeAdded,
    EdgeRemoved,
}

export type QuestEventDagModelChange =
    | QuestEventDagModelNodeAdded
    | QuestEventDagModelNodeRemoved
    | QuestEventDagModelEdgeAdded
    | QuestEventDagModelEdgeRemoved;

export type QuestEventDagModelNodeAdded = {
    type: QuestEventDagModelChangeType.NodeAdded;
};

export type QuestEventDagModelNodeRemoved = {
    type: QuestEventDagModelChangeType.NodeRemoved;
};

export type QuestEventDagModelEdgeAdded = {
    type: QuestEventDagModelChangeType.EdgeAdded;
    parent: QuestEventModel;
    child: QuestEventModel;
};

export type QuestEventDagModelEdgeRemoved = {
    type: QuestEventDagModelChangeType.EdgeRemoved;
    parent: QuestEventModel;
    child: QuestEventModel;
};

type QuestEventDagMeta = {
    /**
     * Index into the array of events. Used for ordering events in sub graphs.
     */
    index: number;
    /**
     * The connected sub graph the event is part of.
     */
    sub_graph: WritableListProperty<QuestEventModel>;
    readonly parents: QuestEventModel[];
    readonly children: QuestEventModel[];
};

/**
 * Events can call each other and form a directed acyclic graph (DAG) this way.
 */
export class QuestEventDagModel implements Observable<QuestEventDagModelChange> {
    private readonly events: QuestEventModel[] = [];
    private readonly _connected_sub_graphs: WritableListProperty<
        WritableListProperty<QuestEventModel>
    > = list_property(element => [element]);
    private readonly meta: Map<QuestEventModel, QuestEventDagMeta> = new Map();
    private readonly emitter: Emitter<QuestEventDagModelChange> = emitter();

    /**
     * The ordering within each sub graph is the same as the ordering within this graph. The
     * ordering of the sub graphs is based on the first event of each sub graph.
     */
    readonly connected_sub_graphs: ListProperty<ListProperty<QuestEventModel>> = this
        ._connected_sub_graphs;

    constructor(public readonly area_id: number) {
        require_non_negative_integer(area_id, "area_id");
    }

    observe(observer: (event: ChangeEvent<QuestEventDagModelChange>) => void): Disposable {
        return this.emitter.observe(observer);
    }

    get_children(event: QuestEventModel): readonly QuestEventModel[] {
        return this.meta.get(event)!.children;
    }

    get_parents(event: QuestEventModel): readonly QuestEventModel[] {
        return this.meta.get(event)!.parents;
    }

    get_index(event: QuestEventModel): number {
        return this.meta.get(event)!.index;
    }

    add_event(
        event: QuestEventModel,
        parents: readonly QuestEventModel[],
        children: readonly QuestEventModel[],
    ): void {
        this.remove_event(event);

        const index = this.events.length;
        const sub_graph = list_property(undefined, event);

        this.meta.set(event, { index, sub_graph, children: [], parents: [] });
        this.events.push(event);

        // Make sure we push the sub graph onto the list of sub graphs after adding the event to the
        // events list to ensure connected_sub_graph observers see consistent state.
        this._connected_sub_graphs.push(sub_graph);

        for (const parent of parents) {
            this.add_edge(parent, event);
        }

        for (const child of children) {
            this.add_edge(event, child);
        }
    }

    insert_event(
        index: number,
        event: QuestEventModel,
        parents: readonly QuestEventModel[],
        children: readonly QuestEventModel[],
    ): void {
        this.remove_event(event);

        const sub_graph = list_property(undefined, event);

        this.meta.set(event, { index, sub_graph, children: [], parents: [] });
        this.events.splice(index, 0, event);

        // Make sure we push the sub graph onto the list of sub graphs after adding the event to the
        // events list to ensure connected_sub_graph observers see consistent state.
        this._connected_sub_graphs.push(sub_graph);

        for (let i = index + 1; i < this.events.length; i++) {
            this.meta.get(this.events[i])!.index = i;
        }

        for (const parent of parents) {
            this.add_edge(parent, event);
        }

        for (const child of children) {
            this.add_edge(event, child);
        }
    }

    remove_event(event: QuestEventModel): void {
        const meta = this.meta.get(event);

        if (meta) {
            // Remove the edges from the event to its children.
            while (meta.children.length) {
                this.internal_remove_edge(
                    event,
                    meta,
                    meta.children[0],
                    this.meta.get(meta.children[0]),
                );
            }

            // Remove the edges from the event to its parents.
            while (meta.parents.length) {
                this.internal_remove_edge(
                    meta.parents[0],
                    this.meta.get(meta.parents[0]),
                    event,
                    meta,
                );
            }

            meta.sub_graph.remove(event);

            if (meta.sub_graph.length.val === 0) {
                this._connected_sub_graphs.remove(meta.sub_graph);
            }

            this.meta.delete(event);
            const index = this.events.indexOf(event);
            this.events.splice(index, 1);

            for (let i = index; i < this.events.length; i++) {
                this.meta.get(this.events[i])!.index = i;
            }
        }
    }

    /**
     * Add a parent-child relationship between two events.
     */
    add_edge(parent: QuestEventModel, child: QuestEventModel): void {
        const parent_meta = this.meta.get(parent);
        defined(parent_meta, "parent is not part of the graph");
        const child_meta = this.meta.get(child);
        defined(child_meta, "child is not part of the graph");

        if (!parent_meta.children.includes(child)) {
            parent_meta.children.push(child);
        }

        if (!child_meta.parents.includes(parent)) {
            child_meta.parents.push(parent);
        }

        const parent_graph = parent_meta.sub_graph;
        const child_graph = child_meta.sub_graph;

        if (parent_graph !== child_graph) {
            // This edge connects two sub graphs. Add the child node's sub graph to the parent
            // node's sub graph, while ensuring correct ordering.
            let insertion_idx = 0;

            for (const event of child_graph) {
                const event_meta = this.meta.get(event)!;

                while (insertion_idx < parent_graph.length.val) {
                    if (this.meta.get(parent_graph.get(insertion_idx))!.index > event_meta.index) {
                        break;
                    } else {
                        insertion_idx++;
                    }
                }

                event_meta.sub_graph = parent_graph;
                parent_graph.splice(insertion_idx, 0, event);
                insertion_idx++;
            }

            this._connected_sub_graphs.remove(child_graph);
            child_graph.clear();
        }

        this.emit({
            type: QuestEventDagModelChangeType.EdgeAdded,
            parent,
            child,
        });
    }

    /**
     * Remove a parent-child relationship between two events.
     */
    remove_edge(parent: QuestEventModel, child: QuestEventModel): void {
        this.internal_remove_edge(parent, this.meta.get(parent), child, this.meta.get(child));
    }

    private internal_remove_edge(
        parent: QuestEventModel,
        parent_meta: QuestEventDagMeta | undefined,
        child: QuestEventModel,
        child_meta: QuestEventDagMeta | undefined,
    ): void {
        if (parent_meta && child_meta) {
            const child_index = parent_meta.children.indexOf(child);

            if (child_index !== -1) {
                parent_meta.children.splice(child_index, 1);
            }

            const parent_index = child_meta.parents.indexOf(parent);

            if (parent_index !== -1) {
                child_meta.parents.splice(parent_index, 1);
            }

            const sub_graph_set = new Set<QuestEventModel>();

            // If the sub graph is not connected anymore, we split it.
            if (!this.connected(parent, child, sub_graph_set)) {
                const sub_graph = list_property<QuestEventModel>(undefined, ...sub_graph_set);
                sub_graph.sort((a, b) => this.meta.get(a)!.index - this.meta.get(b)!.index);

                for (const event of sub_graph) {
                    parent_meta.sub_graph.remove(event);
                }

                const sub_graph_idx = this.meta.get(sub_graph.get(0))!.index;
                let insertion_idx = 0;

                while (insertion_idx < this._connected_sub_graphs.length.val) {
                    const first_event = this._connected_sub_graphs.get(insertion_idx).get(0);
                    const idx = this.meta.get(first_event)!.index;

                    if (idx > sub_graph_idx) {
                        break;
                    } else {
                        insertion_idx++;
                    }
                }

                this._connected_sub_graphs.splice(insertion_idx, 0, sub_graph);
                child_meta.sub_graph = sub_graph;
            }

            this.emit({ type: QuestEventDagModelChangeType.EdgeAdded, parent, child });
        }
    }

    /**
     * Builds up a new sub graph containing node 2 while checking whether node1 and node2 are
     * connected.
     *
     * @returns true if node1 and node2 are connected.
     */
    private connected(
        node1: QuestEventModel,
        node2: QuestEventModel,
        sub_graph: Set<QuestEventModel>,
    ): boolean {
        if (node1 === node2) {
            return true;
        }

        if (sub_graph.has(node2)) {
            return false;
        }

        sub_graph.add(node2);
        const node2_meta = this.meta.get(node2);

        if (node2_meta) {
            for (const child of node2_meta.children) {
                if (this.connected(node1, child, sub_graph)) {
                    return true;
                }
            }

            for (const parent of node2_meta.parents) {
                if (this.connected(node1, parent, sub_graph)) {
                    return true;
                }
            }
        }

        return false;
    }

    private emit(change: QuestEventDagModelChange): void {
        this.emitter.emit({ value: change });
    }
}
