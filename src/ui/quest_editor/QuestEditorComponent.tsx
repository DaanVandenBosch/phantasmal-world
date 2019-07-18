import { Button, Form, Icon, Input, Modal, Select, Upload } from "antd";
import { UploadChangeParam } from "antd/lib/upload";
import { UploadFile } from "antd/lib/upload/interface";
import { observer } from "mobx-react";
import React, { ChangeEvent, ReactNode, Component } from "react";
import { quest_editor_store } from "../../stores/QuestEditorStore";
import { EntityInfoComponent } from "./EntityInfoComponent";
import "./QuestEditorComponent.css";
import { QuestInfoComponent } from "./QuestInfoComponent";
import { RendererComponent } from "../RendererComponent";
import { get_quest_renderer } from "../../rendering/QuestRenderer";
import { application_store } from "../../stores/ApplicationStore";

@observer
export class QuestEditorComponent extends Component<
    {},
    {
        filename?: string;
        save_dialog_open: boolean;
        save_dialog_filename: string;
    }
> {
    state = {
        save_dialog_open: false,
        save_dialog_filename: "Untitled",
    };

    componentDidMount(): void {
        application_store.on_global_keyup("quest_editor", this.keyup);
    }

    render(): ReactNode {
        const quest = quest_editor_store.current_quest;

        return (
            <div className="qe-QuestEditorComponent">
                <Toolbar on_save_as_clicked={this.save_as_clicked} />
                <div className="qe-QuestEditorComponent-main">
                    <QuestInfoComponent quest={quest} />
                    <RendererComponent renderer={get_quest_renderer()} />
                    <EntityInfoComponent entity={quest_editor_store.selected_entity} />
                </div>
                <SaveAsForm
                    is_open={this.state.save_dialog_open}
                    filename={this.state.save_dialog_filename}
                    on_filename_change={this.save_dialog_filename_changed}
                    on_ok={this.save_dialog_affirmed}
                    on_cancel={this.save_dialog_cancelled}
                />
            </div>
        );
    }

    private save_as_clicked = (filename?: string) => {
        const name = filename
            ? filename.endsWith(".qst")
                ? filename.slice(0, -4)
                : filename
            : this.state.save_dialog_filename;

        this.setState({
            save_dialog_open: true,
            save_dialog_filename: name,
        });
    };

    private save_dialog_filename_changed = (filename: string) => {
        this.setState({ save_dialog_filename: filename });
    };

    private save_dialog_affirmed = () => {
        quest_editor_store.save_current_quest_to_file(this.state.save_dialog_filename);
        this.setState({ save_dialog_open: false });
    };

    private save_dialog_cancelled = () => {
        this.setState({ save_dialog_open: false });
    };

    private keyup = (e: KeyboardEvent) => {
        if (e.ctrlKey && e.key === "z" && !e.altKey) {
            quest_editor_store.undo_stack.undo();
        } else if (e.ctrlKey && e.key === "Z" && !e.altKey) {
            quest_editor_store.undo_stack.redo();
        }
    };
}

@observer
class Toolbar extends Component<{ on_save_as_clicked: (filename?: string) => void }> {
    state = {
        filename: undefined,
    };

    render(): ReactNode {
        const undo = quest_editor_store.undo_stack;
        const quest = quest_editor_store.current_quest;
        const areas = quest ? Array.from(quest.area_variants).map(a => a.area) : [];
        const area = quest_editor_store.current_area;
        const area_id = area && area.id;

        return (
            <div className="qe-QuestEditorComponent-toolbar">
                <Upload
                    accept=".qst"
                    showUploadList={false}
                    onChange={this.set_filename}
                    // Make sure it doesn't do a POST:
                    customRequest={() => false}
                >
                    <Button icon="file">{this.state.filename || "Open file..."}</Button>
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
                <Button icon="save" onClick={this.save_as} disabled={!quest}>
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
            </div>
        );
    }

    private set_filename = (info: UploadChangeParam<UploadFile>) => {
        if (info.file.originFileObj) {
            this.setState({ filename: info.file.name });
            quest_editor_store.load_file(info.file.originFileObj as File);
        }
    };

    private save_as = () => {
        this.props.on_save_as_clicked(this.state.filename);
    };

    private undo = () => {
        quest_editor_store.undo_stack.undo();
    };

    private redo = () => {
        quest_editor_store.undo_stack.redo();
    };
}

class SaveAsForm extends React.Component<{
    is_open: boolean;
    filename: string;
    on_filename_change: (name: string) => void;
    on_ok: () => void;
    on_cancel: () => void;
}> {
    render(): ReactNode {
        return (
            <Modal
                title={
                    <>
                        <Icon type="save" /> Save as...
                    </>
                }
                visible={this.props.is_open}
                onOk={this.props.on_ok}
                onCancel={this.props.on_cancel}
            >
                <Form layout="vertical">
                    <Form.Item label="Name">
                        <Input
                            autoFocus={true}
                            maxLength={12}
                            value={this.props.filename}
                            onChange={this.name_changed}
                        />
                    </Form.Item>
                </Form>
            </Modal>
        );
    }

    private name_changed = (e: ChangeEvent<HTMLInputElement>) => {
        this.props.on_filename_change(e.currentTarget.value);
    };
}
