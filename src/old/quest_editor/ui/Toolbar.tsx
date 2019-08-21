import { Button, Dropdown, Form, Icon, Input, Menu, Modal, Select, Upload } from "antd";
import { ClickParam } from "antd/lib/menu";
import { UploadChangeParam, UploadFile } from "antd/lib/upload/interface";
import { observer } from "mobx-react";
import React, { ChangeEvent, Component, ReactNode } from "react";
import { area_store } from "../stores/AreaStore";
import { quest_editor_store } from "../stores/QuestEditorStore";
import { undo_manager } from "../../core/undo";
import styles from "./Toolbar.css";
import { Episode } from "../../../core/data_formats/parsing/quest/Episode";

@observer
export class Toolbar extends Component {
    render(): ReactNode {
        const quest = quest_editor_store.current_quest;

        return (
            <div className={styles.main}>
                <Dropdown
                    overlay={
                        <Menu onClick={this.new_quest}>
                            <Menu.Item key={Episode[Episode.I]}>Episode I</Menu.Item>
                        </Menu>
                    }
                    trigger={["click"]}
                >
                    <Button icon="file-add">
                        New quest
                        <Icon type="down" />
                    </Button>
                </Dropdown>
                <Upload
                    accept=".qst"
                    showUploadList={false}
                    onChange={this.open_file}
                    // Make sure it doesn't do a POST:
                    customRequest={() => false}
                >
                    <Button icon="file">Open file...</Button>
                </Upload>
                <Button icon="save" onClick={quest_editor_store.open_save_dialog} disabled={!quest}>
                    Save as...
                </Button>
                <Button
                    icon="undo"
                    onClick={this.undo}
                    title={
                        undo_manager.first_undo
                            ? `Undo "${undo_manager.first_undo.description}"`
                            : "Nothing to undo"
                    }
                    disabled={!undo_manager.can_undo}
                >
                    Undo
                </Button>
                <Button
                    icon="redo"
                    onClick={this.redo}
                    title={
                        undo_manager.first_redo
                            ? `Redo "${undo_manager.first_redo.description}"`
                            : "Nothing to redo"
                    }
                    disabled={!undo_manager.can_redo}
                >
                    Redo
                </Button>
                <AreaComponent />
                <SaveQuestComponent />
            </div>
        );
    }

    private new_quest({ key }: ClickParam): void {
        quest_editor_store.new_quest((Episode as any)[key]);
    }

    private open_file(info: UploadChangeParam<UploadFile>): void {
        if (info.file.originFileObj) {
            quest_editor_store.open_file(info.file.name, info.file.originFileObj as File);
        }
    }

    private undo(): void {
        undo_manager.undo();
    }

    private redo(): void {
        undo_manager.redo();
    }
}

@observer
class AreaComponent extends Component {
    render(): ReactNode {
        const quest = quest_editor_store.current_quest;
        const areas = quest ? area_store.get_areas_for_episode(quest.episode) : [];
        const area = quest_editor_store.current_area;

        return (
            <Select
                onChange={quest_editor_store.set_current_area_id}
                value={area && area.id}
                style={{ width: 200 }}
                disabled={!quest}
            >
                {areas.map(area => {
                    const entity_count = quest && quest.entities_per_area.get(area.id);
                    return (
                        <Select.Option key={area.id} value={area.id}>
                            {area.name}
                            {entity_count && ` (${entity_count})`}
                        </Select.Option>
                    );
                })}
            </Select>
        );
    }
}

@observer
class SaveQuestComponent extends Component {
    render(): ReactNode {
        return (
            <Modal
                title={
                    <>
                        <Icon type="save" /> Save as...
                    </>
                }
                visible={quest_editor_store.save_dialog_open}
                onOk={this.ok}
                onCancel={this.cancel}
            >
                <Form layout="vertical">
                    <Form.Item label="Name">
                        <Input
                            autoFocus={true}
                            maxLength={32}
                            value={quest_editor_store.save_dialog_filename}
                            onChange={this.name_changed}
                        />
                    </Form.Item>
                </Form>
            </Modal>
        );
    }

    private name_changed(e: ChangeEvent<HTMLInputElement>): void {
        quest_editor_store.set_save_dialog_filename(e.currentTarget.value);
    }

    private ok(): void {
        quest_editor_store.save_current_quest_to_file(
            quest_editor_store.save_dialog_filename || "untitled",
        );
    }

    private cancel(): void {
        quest_editor_store.close_save_dialog();
    }
}
