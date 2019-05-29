import React, { CSSProperties } from 'react';
import { NpcType, Quest } from '../domain';

const containerStyle: CSSProperties = {
    width: 280,
    padding: 10,
    display: 'flex',
    flexDirection: 'column'
};

const tableStyle: CSSProperties = {
    borderCollapse: 'collapse',
    width: '100%'
};

const tableHeaderStyle: CSSProperties = {
    textAlign: 'right',
    paddingRight: 5
};

const descriptionStyle: CSSProperties = {
    whiteSpace: 'pre-wrap',
    margin: '3px 0 3px 0'
};

const npcCountsContainerStyle: CSSProperties = {
    overflow: 'auto'
};

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
            <div style={containerStyle}>
                <table style={tableStyle}>
                    <tbody>
                        <tr>
                            <th style={tableHeaderStyle}>Name:</th><td>{quest.name}</td>
                        </tr>
                        <tr>
                            <th style={tableHeaderStyle}>Episode:</th><td>{episode}</td>
                        </tr>
                        <tr>
                            <td colSpan={2}>
                                <pre className="bp3-code-block" style={descriptionStyle}>{quest.shortDescription}</pre>
                            </td>
                        </tr>
                        <tr>
                            <td colSpan={2}>
                                <pre className="bp3-code-block" style={descriptionStyle}>{quest.longDescription}</pre>
                            </td>
                        </tr>
                    </tbody>
                </table>
                <div style={npcCountsContainerStyle}>
                    <table style={tableStyle}>
                        <thead>
                            <tr><th>NPC Counts</th></tr>
                        </thead>
                        <tbody>
                            {npcCountRows}
                        </tbody>
                    </table>
                </div>
            </div>
        );
    } else {
        return <div style={containerStyle} />;
    }
}
