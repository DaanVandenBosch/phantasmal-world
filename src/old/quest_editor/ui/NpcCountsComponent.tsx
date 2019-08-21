import React, { Component, ReactNode } from "react";
import styles from "./NpcCountsComponent.css";
import { npc_data, NpcType } from "../../../core/data_formats/parsing/quest/npc_types";
import { quest_editor_store } from "../stores/QuestEditorStore";
import { observer } from "mobx-react";

@observer
export class NpcCountsComponent extends Component {
    render(): ReactNode {
        const quest = quest_editor_store.current_quest;
        const npc_counts = new Map<NpcType, number>();

        if (quest) {
            for (const npc of quest.npcs) {
                const val = npc_counts.get(npc.type) || 0;
                npc_counts.set(npc.type, val + 1);
            }
        }

        const extra_canadines = (npc_counts.get(NpcType.Canane) || 0) * 8;

        // Sort by canonical order.
        const sorted_npc_counts = [...npc_counts].sort((a, b) => a[0] - b[0]);

        const npc_count_rows = sorted_npc_counts.map(([npc_type, count]) => {
            const extra = npc_type === NpcType.Canadine ? extra_canadines : 0;
            return (
                <tr key={npc_type}>
                    <td>{npc_data(npc_type).name}:</td>
                    <td>{count + extra}</td>
                </tr>
            );
        });

        return (
            <div className={styles.main}>
                <table>
                    <tbody>{npc_count_rows}</tbody>
                </table>
            </div>
        );
    }
}
