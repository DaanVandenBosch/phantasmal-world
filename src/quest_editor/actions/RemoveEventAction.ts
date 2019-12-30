import { Action } from "../../core/undo/Action";
import { QuestEventModel } from "../model/QuestEventModel";
import { QuestEventDagModel } from "../model/QuestEventDagModel";
import { QuestEditorStore } from "../stores/QuestEditorStore";
import { QuestModel } from "../model/QuestModel";
import { QuestNpcModel } from "../model/QuestNpcModel";

export class RemoveEventAction implements Action {
    private readonly parents: readonly QuestEventModel[];
    private readonly children: readonly QuestEventModel[];
    /**
     * The event's parents will be connected to the event's children.
     */
    private readonly new_edges: { parent: QuestEventModel; child: QuestEventModel }[] = [];
    /**
     * NPCs who's waves have been removed.
     */
    private readonly npcs: readonly QuestNpcModel[];
    private readonly event_index: number;

    readonly description: string;

    constructor(
        private readonly store: QuestEditorStore,
        private readonly quest: QuestModel,
        private readonly event_dag: QuestEventDagModel,
        private readonly event: QuestEventModel,
    ) {
        this.description = `Delete event ${event.id}`;

        this.parents = event_dag.get_parents(event).slice();
        this.children = event_dag.get_children(event).slice();

        for (const parent of this.parents) {
            const siblings = event_dag.get_children(parent);

            for (const child of this.children) {
                if (!siblings.includes(child)) {
                    this.new_edges.push({ parent, child });
                }
            }
        }

        this.npcs = quest.npcs.val.filter(npc => npc.wave.val === event.wave);

        this.event_index = event_dag.get_index(event);
    }

    undo(): void {
        this.quest.insert_event(this.event_index, this.event, this.parents, this.children);

        for (const { parent, child } of this.new_edges) {
            this.event_dag.remove_edge(parent, child);
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
