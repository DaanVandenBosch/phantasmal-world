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
}
