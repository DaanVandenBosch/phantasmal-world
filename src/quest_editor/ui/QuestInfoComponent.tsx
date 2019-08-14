import { observer } from "mobx-react";
import React, { ChangeEvent, Component, ReactNode } from "react";
import { quest_editor_store } from "../stores/QuestEditorStore";
import { DisabledTextComponent } from "../../core/ui/DisabledTextComponent";
import styles from "./QuestInfoComponent.css";
import { Episode } from "../../core/data_formats/parsing/quest/Episode";
import { NumberInput } from "../../core/ui/NumberInput";
import { TextInput } from "../../core/ui/TextInput";
import { TextArea } from "../../core/ui/TextArea";

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
                                <NumberInput
                                    value={quest.id}
                                    min={0}
                                    max={4294967295}
                                    on_change={this.id_changed}
                                />
                            </td>
                        </tr>
                        <tr>
                            <th>Name:</th>
                            <td>
                                <TextInput
                                    value={quest.name}
                                    max_length={32}
                                    on_change={this.name_changed}
                                />
                            </td>
                        </tr>
                        <tr>
                            <th colSpan={2}>Short description:</th>
                        </tr>
                        <tr>
                            <td colSpan={2}>
                                <TextArea
                                    value={quest.short_description}
                                    max_length={128}
                                    rows={3}
                                    on_change={this.short_description_changed}
                                />
                            </td>
                        </tr>
                        <tr>
                            <th colSpan={2}>Long description:</th>
                        </tr>
                        <tr>
                            <td colSpan={2}>
                                <TextArea
                                    value={quest.long_description}
                                    max_length={288}
                                    rows={5}
                                    on_change={this.long_description_changed}
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
            quest_editor_store.push_id_edit_action(quest.id, value);
        }
    }

    private name_changed(e: ChangeEvent<HTMLInputElement>): void {
        const quest = quest_editor_store.current_quest;

        if (quest) {
            quest_editor_store.push_name_edit_action(quest.name, e.currentTarget.value);
        }
    }

    private short_description_changed(e: ChangeEvent<HTMLTextAreaElement>): void {
        const quest = quest_editor_store.current_quest;

        if (quest) {
            quest_editor_store.push_short_description_edit_action(
                quest.short_description,
                e.currentTarget.value,
            );
        }
    }

    private long_description_changed(e: ChangeEvent<HTMLTextAreaElement>): void {
        const quest = quest_editor_store.current_quest;

        if (quest) {
            quest_editor_store.push_long_description_edit_action(
                quest.long_description,
                e.currentTarget.value,
            );
        }
    }
}
