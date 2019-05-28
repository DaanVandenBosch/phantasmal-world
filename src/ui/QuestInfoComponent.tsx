import React, { CSSProperties } from 'react';
import { NpcType, Quest } from '../domain';

const container_style: CSSProperties = {
    width: 280,
    padding: 10,
    display: 'flex',
    flexDirection: 'column'
};

const table_style: CSSProperties = {
    borderCollapse: 'collapse',
    width: '100%'
};

const table_header_style: CSSProperties = {
    textAlign: 'right',
    paddingRight: 5
};

const description_style: CSSProperties = {
    whiteSpace: 'pre-wrap',
    margin: '3px 0 3px 0'
};

const npc_counts_container_style: CSSProperties = {
    overflow: 'auto'
};

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
            <div style={container_style}>
                <table style={table_style}>
                    <tbody>
                        <tr>
                            <th style={table_header_style}>Name:</th><td>{quest.name}</td>
                        </tr>
                        <tr>
                            <th style={table_header_style}>Episode:</th><td>{episode}</td>
                        </tr>
                        <tr>
                            <td colSpan={2}>
                                <pre className="bp3-code-block" style={description_style}>{quest.shortDescription}</pre>
                            </td>
                        </tr>
                        <tr>
                            <td colSpan={2}>
                                <pre className="bp3-code-block" style={description_style}>{quest.longDescription}</pre>
                            </td>
                        </tr>
                    </tbody>
                </table>
                <div style={npc_counts_container_style}>
                    <table style={table_style}>
                        <thead>
                            <tr><th>NPC Counts</th></tr>
                        </thead>
                        <tbody>
                            {npc_count_rows}
                        </tbody>
                    </table>
                </div>
            </div>
        );
    } else {
        return <div style={container_style} />;
    }
}
