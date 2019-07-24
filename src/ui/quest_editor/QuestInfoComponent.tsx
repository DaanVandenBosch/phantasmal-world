import { observer } from "mobx-react";
import React, { Component, ReactNode } from "react";
import { NpcType } from "../../domain";
import { quest_editor_store } from "../../stores/QuestEditorStore";
import "./QuestInfoComponent.css";
import { DisabledTextComponent } from "../DisabledTextComponent";

@observer
export class QuestInfoComponent extends Component {
    render(): ReactNode {
        const quest = quest_editor_store.current_quest;
        let body: ReactNode;

        if (quest) {
            const episode = quest.episode === 4 ? "IV" : quest.episode === 2 ? "II" : "I";
            const npc_counts = new Map<NpcType, number>();

            for (const npc of quest.npcs) {
                const val = npc_counts.get(npc.type) || 0;
                npc_counts.set(npc.type, val + 1);
            }

            const extra_canadines = (npc_counts.get(NpcType.Canane) || 0) * 8;

            // Sort by type ID.
            const sorted_npc_counts = [...npc_counts].sort((a, b) => a[0].id - b[0].id);

            const npc_count_rows = sorted_npc_counts.map(([npc_type, count]) => {
                const extra = npc_type === NpcType.Canadine ? extra_canadines : 0;
                return (
                    <tr key={npc_type.id}>
                        <td>{npc_type.name}:</td>
                        <td>{count + extra}</td>
                    </tr>
                );
            });

            body = (
                <>
                    <table>
                        <tbody>
                            <tr>
                                <th>Name:</th>
                                <td>{quest.name}</td>
                            </tr>
                            <tr>
                                <th>Episode:</th>
                                <td>{episode}</td>
                            </tr>
                            <tr>
                                <td colSpan={2}>
                                    <pre>{quest.short_description}</pre>
                                </td>
                            </tr>
                            <tr>
                                <td colSpan={2}>
                                    <pre>{quest.long_description}</pre>
                                </td>
                            </tr>
                        </tbody>
                    </table>
                    <div className="qe-QuestInfoComponent-npc-counts-container">
                        <table>
                            <thead>
                                <tr>
                                    <th colSpan={2}>NPC Counts</th>
                                </tr>
                            </thead>
                            <tbody>{npc_count_rows}</tbody>
                        </table>
                    </div>
                </>
            );
        } else {
            body = <DisabledTextComponent>No quest loaded.</DisabledTextComponent>;
        }

        return (
            <div className="qe-QuestInfoComponent" tabIndex={-1}>
                {body}
            </div>
        );
    }
}
