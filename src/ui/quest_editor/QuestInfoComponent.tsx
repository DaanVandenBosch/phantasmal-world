import React from 'react';
import { NpcType, Quest } from '../../domain';
import './QuestInfoComponent.css';

export function QuestInfoComponent({ quest }: { quest?: Quest }) {
    if (quest) {
        const episode = quest.episode === 4 ? 'IV' : (quest.episode === 2 ? 'II' : 'I');
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

        return (
            <div className="qe-QuestInfoComponent">
                <table>
                    <tbody>
                        <tr>
                            <th>Name:</th><td>{quest.name}</td>
                        </tr>
                        <tr>
                            <th>Episode:</th><td>{episode}</td>
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
                    <table >
                        <thead>
                            <tr><th colSpan={2}>NPC Counts</th></tr>
                        </thead>
                        <tbody>
                            {npc_count_rows}
                        </tbody>
                    </table>
                </div>
            </div>
        );
    } else {
        return <div className="qe-QuestInfoComponent" />;
    }
}
