import { Input, InputNumber } from "antd";
import { observer } from "mobx-react";
import React, { ChangeEvent, Component, ReactNode } from "react";
import { quest_editor_store } from "../stores/QuestEditorStore";
import { DisabledTextComponent } from "../../core/ui/DisabledTextComponent";
import styles from "./QuestInfoComponent.css";
import { Episode } from "../../core/data_formats/parsing/quest/Episode";

@observer
export class QuestInfoComponent extends Component {
    render(): ReactNode {
        const quest = quest_editor_store.current_quest;
        let body: ReactNode;

        if (quest) {
            const episode =
                quest.episode === Episode.IV ? "IV" : quest.episode === Episode.II ? "II" : "I";

            body = (
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
