import { Button, Dropdown, Form, Icon, Input, Menu, Modal, Select, Upload } from "antd";
import { UploadChangeParam, UploadFile } from "antd/lib/upload/interface";
import { observer } from "mobx-react";
import React, { ChangeEvent, Component, ReactNode } from "react";
import { Episode } from "../../domain";
import { quest_editor_store } from "../../stores/QuestEditorStore";
import "./Toolbar.less";
import { ClickParam } from "antd/lib/menu";

@observer
export class Toolbar extends Component {
    render(): ReactNode {
        const undo = quest_editor_store.undo_stack;
        const quest = quest_editor_store.current_quest;
        const areas = quest ? Array.from(quest.area_variants).map(a => a.area) : [];
        const area = quest_editor_store.current_area;
        const area_id = area && area.id;

        return (
            <div className="qe-Toolbar">
                <Dropdown
                    overlay={
                        <Menu onClick={this.new_quest}>
                            <Menu.Item key={Episode[Episode.I]}>Episode I</Menu.Item>
                            <Menu.Item key={Episode[Episode.II]}>Episode II</Menu.Item>
                            <Menu.Item key={Episode[Episode.IV]}>Episode IV</Menu.Item>
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
                    <Button icon="file">
                        {quest_editor_store.current_quest_filename || "Open file..."}
                    </Button>
                </Upload>
                <Select
                    onChange={quest_editor_store.set_current_area_id}
                    value={area_id}
                    style={{ width: 200 }}
                    disabled={!quest}
                >
                    {areas.map(area => (
                        <Select.Option key={area.id} value={area.id}>
                            {area.name}
                        </Select.Option>
                    ))}
                </Select>
                <Button icon="save" onClick={quest_editor_store.open_save_dialog} disabled={!quest}>
                    Save as...
                </Button>
                <Button
                    icon="undo"
                    onClick={this.undo}
                    title={"Undo" + (undo.first_undo ? ` "${undo.first_undo.description}"` : "")}
                    disabled={!undo.can_undo}
                />
                <Button
                    icon="redo"
                    onClick={this.redo}
                    title={"Redo" + (undo.first_redo ? ` "${undo.first_redo.description}"` : "")}
                    disabled={!quest_editor_store.undo_stack.can_redo}
                />
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
        quest_editor_store.undo_stack.undo();
    }

    private redo(): void {
        quest_editor_store.undo_stack.redo();
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
            quest_editor_store.save_dialog_filename || "untitled"
        );
    }

    private cancel(): void {
        quest_editor_store.close_save_dialog();
    }
}
