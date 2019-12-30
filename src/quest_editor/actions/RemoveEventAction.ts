import { Action } from "../../core/undo/Action";
import { QuestEventModel } from "../model/QuestEventModel";
import { QuestEventDagModel } from "../model/QuestEventDagModel";
import { QuestEditorStore } from "../stores/QuestEditorStore";
import { QuestModel } from "../model/QuestModel";
import { QuestNpcModel } from "../model/QuestNpcModel";

export class RemoveEventAction implements Action {
    private readonly children: readonly QuestEventModel[];
    private readonly parents: readonly QuestEventModel[];
    /**
     * The event's parents will be connected to the event's children.
     */
    private readonly new_edges: { parent: QuestEventModel; child: QuestEventModel }[] = [];
    /**
     * NPCs who's waves have been removed.
     */
    private readonly npcs: readonly QuestNpcModel[];
    private readonly event_index: number;
    /**
     * The entire DAG will be removed from the quest if this event is the last one in it. Remember
     * whether this happened so we can restore the DAG too if needed.
     */
    private readonly dag_index?: number;

    readonly description: string;

    constructor(
        private readonly store: QuestEditorStore,
        private readonly quest: QuestModel,
        private readonly event_dag: QuestEventDagModel,
        private readonly event: QuestEventModel,
    ) {
        this.description = `Delete event ${event.id}`;

        this.children = event_dag.get_children(event).slice();
        this.parents = event_dag.get_parents(event).slice();

        for (const parent of this.parents) {
            const siblings = event_dag.get_children(parent);

            for (const child of this.children) {
                if (!siblings.includes(child)) {
                    this.new_edges.push({ parent, child });
                }
            }
        }

        this.npcs = quest.npcs.val.filter(npc => npc.wave.val === event.wave);

        if (event_dag.events.length.val === 1) {
            this.dag_index = quest.event_dags.val.indexOf(event_dag);
        }

        this.event_index = event_dag.events.val.indexOf(event);
    }

    undo(): void {
        for (const { parent, child } of this.new_edges) {
            this.event_dag.remove_edge(parent, child);
        }

        this.event_dag.add_event_at(this.event_index, this.event, this.children, this.parents);

        if (this.dag_index != undefined) {
            this.quest.add_event_dag_at(this.dag_index, this.event_dag);
        }

        for (const npc of this.npcs) {
            npc.set_wave(this.event.wave);
        }
    }

    redo(): void {
        if (this.store.selected_wave.val === this.event.wave) {
            this.store.set_selected_wave(undefined);
        }

        // Connect event's parents to its children.
        for (const { parent, child } of this.new_edges) {
            this.event_dag.add_edge(parent, child);
        }

        this.quest.remove_event(this.event_dag, this.event);
    }
}
