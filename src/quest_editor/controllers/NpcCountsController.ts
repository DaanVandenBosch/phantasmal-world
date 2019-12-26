import { Controller } from "../../core/controllers/Controller";
import { QuestEditorStore } from "../stores/QuestEditorStore";
import { Property } from "../../core/observable/property/Property";
import { QuestNpcModel } from "../model/QuestNpcModel";
import { npc_data, NpcType } from "../../core/data_formats/parsing/quest/npc_types";
import { property } from "../../core/observable";

export type NameWithCount = { readonly name: string; readonly count: number };

export class NpcCountsController extends Controller {
    readonly npc_counts: Property<readonly NameWithCount[]>;
    readonly unavailable: Property<boolean>;

    constructor(store: QuestEditorStore) {
        super();

        this.unavailable = store.current_quest.map(q => q == undefined);

        this.npc_counts = store.current_quest
            .flat_map(quest => (quest ? quest.npcs : property([])))
            .map(this.update_view);
    }

    private update_view(npcs: readonly QuestNpcModel[]): NameWithCount[] {
        const npc_counts = new Map<NpcType, number>();

        for (const npc of npcs) {
            const val = npc_counts.get(npc.type) || 0;
            npc_counts.set(npc.type, val + 1);
        }

        const extra_canadines = (npc_counts.get(NpcType.Canane) || 0) * 8;

        // Sort by canonical order.
        const sorted_npc_counts = [...npc_counts].sort((a, b) => a[0] - b[0]);

        return sorted_npc_counts.map(([npc_type, count]) => {
            const extra = npc_type === NpcType.Canadine ? extra_canadines : 0;

            return { name: npc_data(npc_type).name, count: count + extra };
        });
    }
}
