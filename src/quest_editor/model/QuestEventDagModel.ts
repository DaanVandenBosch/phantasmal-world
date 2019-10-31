import { QuestEventModel } from "./QuestEventModel";
import { ListProperty } from "../../core/observable/property/list/ListProperty";
import { WritableListProperty } from "../../core/observable/property/list/WritableListProperty";
import { list_property } from "../../core/observable";

export type QuestEventDagModelMeta = {
    parents: QuestEventModel[];
    children: QuestEventModel[];
};

/**
 * Events can call each other and form a directed acyclic graph (DAG) this way.
 */
export class QuestEventDagModel {
    private readonly _root_events: WritableListProperty<QuestEventModel>;
    private meta: Map<QuestEventModel, QuestEventDagModelMeta>;

    readonly area_id: number;

    readonly events: QuestEventModel[];

    /**
     * The root nodes of the DAG. These events are not called from any other events.
     */
    readonly root_events: ListProperty<QuestEventModel>;

    constructor(
        area_id: number,
        events: QuestEventModel[],
        root_events: QuestEventModel[],
        meta: Map<QuestEventModel, QuestEventDagModelMeta>,
    ) {
        if (!Number.isInteger(area_id)) throw new Error("area_id should be an integer.");
        if (!Array.isArray(events)) throw new Error("events should be an array.");
        if (!Array.isArray(root_events)) throw new Error("root_events should be an array.");
        if (!meta) throw new Error("meta is required.");

        this.area_id = area_id;
        this.events = events;
        this._root_events = list_property(undefined, ...root_events);
        this.root_events = this._root_events;
        this.meta = meta;
    }

    get_children(event: QuestEventModel): readonly QuestEventModel[] {
        const meta = this.meta.get(event);
        return meta ? meta.children : [];
    }
}
