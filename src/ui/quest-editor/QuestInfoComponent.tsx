import React from 'react';
import { NpcType, Quest } from '../../domain';
import './QuestInfoComponent.css';

export function QuestInfoComponent({ quest }: { quest?: Quest }) {
    if (quest) {
        const episode = quest.episode === 4 ? 'IV' : (quest.episode === 2 ? 'II' : 'I');
        const npcCounts = new Map<NpcType, number>();

        for (const npc of quest.npcs) {
            const val = npcCounts.get(npc.type) || 0;
            npcCounts.set(npc.type, val + 1);
        }

        const extraCanadines = (npcCounts.get(NpcType.Canane) || 0) * 8;

        // Sort by type ID.
        const sortedNpcCounts = [...npcCounts].sort((a, b) => a[0].id - b[0].id);

        const npcCountRows = sortedNpcCounts.map(([npcType, count]) => {
            const extra = npcType === NpcType.Canadine ? extraCanadines : 0;
            return (
                <tr key={npcType.id}>
                    <td>{npcType.name}:</td>
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
                                <pre>{quest.shortDescription}</pre>
                            </td>
                        </tr>
                        <tr>
                            <td colSpan={2}>
                                <pre>{quest.longDescription}</pre>
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
                            {npcCountRows}
                        </tbody>
                    </table>
                </div>
            </div>
        );
    } else {
        return <div className="qe-QuestInfoComponent" />;
    }
}
