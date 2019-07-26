import { Input, InputNumber } from "antd";
import { observer } from "mobx-react";
import React, { ChangeEvent, Component, ReactNode } from "react";
import { Episode, NpcType } from "../../domain";
import { quest_editor_store } from "../../stores/QuestEditorStore";
import { DisabledTextComponent } from "../DisabledTextComponent";
import styles from "./QuestInfoComponent.css";

@observer
export class QuestInfoComponent extends Component {
    render(): ReactNode {
        const quest = quest_editor_store.current_quest;
        let body: ReactNode;

        if (quest) {
            const episode =
                quest.episode === Episode.IV ? "IV" : quest.episode === Episode.II ? "II" : "I";
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
                                <th>Episode:</th>
                                <td>{episode}</td>
                            </tr>
                            <tr>
                                <th>ID:</th>
                                <td>
                                    <InputNumber
                                        value={quest.id}
                                        max={4294967295}
                                        min={0}
                                        onChange={this.id_changed}
                                        size="small"
                                    />
                                </td>
                            </tr>
                            <tr>
                                <th>Name:</th>
                                <td>
                                    <Input
                                        value={quest.name}
                                        maxLength={32}
                                        onChange={this.name_changed}
                                        size="small"
                                    />
                                </td>
                            </tr>
                            <tr>
                                <th colSpan={2}>Short description:</th>
                            </tr>
                            <tr>
                                <td colSpan={2}>
                                    <Input.TextArea
                                        value={quest.short_description}
                                        maxLength={128}
                                        rows={3}
                                        onChange={this.short_description_changed}
                                    />
                                </td>
                            </tr>
                            <tr>
                                <th colSpan={2}>Long description:</th>
                            </tr>
                            <tr>
                                <td colSpan={2}>
                                    <Input.TextArea
                                        value={quest.long_description}
                                        maxLength={288}
                                        rows={5}
                                        onChange={this.long_description_changed}
                                    />
                                </td>
                            </tr>
                        </tbody>
                    </table>
                    <div className={styles.npc_counts_container}>
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
            <div className={styles.main} tabIndex={-1}>
                {body}
            </div>
        );
    }

    private id_changed(value?: number): void {
        const quest = quest_editor_store.current_quest;

        if (quest && value != undefined) {
            quest.set_id(value);
        }
    }

    private name_changed(e: ChangeEvent<HTMLInputElement>): void {
        const quest = quest_editor_store.current_quest;

        if (quest) {
            quest.set_name(e.target.value);
        }
    }

    private short_description_changed(e: ChangeEvent<HTMLTextAreaElement>): void {
        const quest = quest_editor_store.current_quest;

        if (quest) {
            quest.set_short_description(e.target.value);
        }
    }

    private long_description_changed(e: ChangeEvent<HTMLTextAreaElement>): void {
        const quest = quest_editor_store.current_quest;

        if (quest) {
            quest.set_long_description(e.target.value);
        }
    }
}
