import { Button, Form, Icon, Input, Modal, Select, Upload } from "antd";
import { UploadChangeParam } from "antd/lib/upload";
import { UploadFile } from "antd/lib/upload/interface";
import { observer } from "mobx-react";
import React, { ChangeEvent } from "react";
import { loadFile } from "../../actions/quest-editor/loadFile";
import { saveCurrentQuestToFile, setCurrentAreaId } from "../../actions/quest-editor/questEditor";
import { questEditorStore } from "../../stores/QuestEditorStore";
import { EntityInfoComponent } from "./EntityInfoComponent";
import './QuestEditorComponent.css';
import { QuestInfoComponent } from "./QuestInfoComponent";
import { RendererComponent } from "./RendererComponent";

@observer
export class QuestEditorComponent extends React.Component<{}, {
    filename?: string,
    saveDialogOpen: boolean,
    saveDialogFilename: string
}> {
    state = {
        saveDialogOpen: false,
        saveDialogFilename: 'Untitled',
    };

    render() {
        const quest = questEditorStore.currentQuest;
        const model = questEditorStore.currentModel;
        const area = questEditorStore.currentArea;

        return (
            <div className="qe-QuestEditorComponent">
                <Toolbar onSaveAsClicked={this.saveAsClicked} />
                <div className="qe-QuestEditorComponent-main">
                    <QuestInfoComponent quest={quest} />
                    <RendererComponent
                        quest={quest}
                        area={area}
                        model={model}
                    />
                    <EntityInfoComponent entity={questEditorStore.selectedEntity} />
                </div>
                <SaveAsForm
                    isOpen={this.state.saveDialogOpen}
                    filename={this.state.saveDialogFilename}
                    onFilenameChange={this.saveDialogFilenameChanged}
                    onOk={this.saveDialogAffirmed}
                    onCancel={this.saveDialogCancelled}
                />
            </div>
        );
    }

    private saveAsClicked = (filename: string) => {
        const name = filename.endsWith('.qst') ? filename.slice(0, -4) : filename;

        this.setState({
            saveDialogOpen: true,
            saveDialogFilename: name
        });
    }

    private saveDialogFilenameChanged = (filename: string) => {
        this.setState({ saveDialogFilename: filename });
    }

    private saveDialogAffirmed = () => {
        saveCurrentQuestToFile(this.state.saveDialogFilename);
        this.setState({ saveDialogOpen: false });
    }

    private saveDialogCancelled = () => {
        this.setState({ saveDialogOpen: false });
    }
}

@observer
class Toolbar extends React.Component<{ onSaveAsClicked: (filename: string) => void }> {
    state = {
        filename: 'Choose file...'
    }

    render() {
        const quest = questEditorStore.currentQuest;
        const areas = quest && Array.from(quest.areaVariants).map(a => a.area);
        const area = questEditorStore.currentArea;
        const areaId = area && area.id;

        return (
            <div className="qe-QuestEditorComponent-toolbar">
                <Upload
                    accept=".nj, .qst, .xj"
                    showUploadList={false}
                    onChange={this.setFilename}
                >
                    <Button icon="file">{this.state.filename}</Button>
                </Upload>
                {areas && (
                    <Select
                        onChange={setCurrentAreaId}
                        defaultValue={areaId}
                        style={{ width: 200 }}
                    >
                        {areas.map(area =>
                            <Select.Option key={area.id} value={area.id}>{area.name}</Select.Option>
                        )}
                    </Select>
                )}
                {quest && (
                    <Button
                        icon="save"
                        onClick={this.saveAsClicked}
                    >Save as...</Button>
                )}
            </div>
        );
    }

    private setFilename = (info: UploadChangeParam<UploadFile>) => {
        if (info.file.originFileObj) {
            this.setState({ filename: info.file.name });
            loadFile(info.file.originFileObj);
        }
    }

    private saveAsClicked = () => {
        this.props.onSaveAsClicked(this.state.filename);
    }
}

class SaveAsForm extends React.Component<{
    isOpen: boolean,
    filename: string,
    onFilenameChange: (name: string) => void,
    onOk: () => void,
    onCancel: () => void
}> {
    render() {
        return (
            <Modal
                title={<><Icon type="save" /> Save as...</>}
                visible={this.props.isOpen}
                onOk={this.props.onOk}
                onCancel={this.props.onCancel}
            >
                <Form layout="vertical">
                    <Form.Item label="Name">
                        <Input
                            autoFocus={true}
                            maxLength={12}
                            value={this.props.filename}
                            onChange={this.nameChanged}
                        />
                    </Form.Item>
                </Form>
            </Modal>
        );
    }

    private nameChanged = (e: ChangeEvent<HTMLInputElement>) => {
        this.props.onFilenameChange(e.currentTarget.value);
    }
}
