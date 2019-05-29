import { Button, Classes, Dialog, FileInput, FormGroup, HTMLSelect, InputGroup, Intent, Navbar, NavbarGroup } from "@blueprintjs/core";
import { observer } from "mobx-react";
import React, { ChangeEvent, KeyboardEvent } from "react";
import { saveCurrentQuestToFile, setCurrentAreaId } from "../../actions/quest-editor/questEditor";
import { loadFile } from "../../actions/quest-editor/loadFile";
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
        filename: undefined,
        saveDialogOpen: false,
        saveDialogFilename: 'Untitled',
    };

    render() {
        const quest = questEditorStore.currentQuest;
        const model = questEditorStore.currentModel;
        const areas = quest && Array.from(quest.areaVariants).map(a => a.area);
        const area = questEditorStore.currentArea;
        const areaId = area && String(area.id);

        return (
            <div className="QuestEditorComponent">
                <Navbar>
                    <NavbarGroup className="QuestEditorComponent-button-bar">
                        <FileInput
                            text={this.state.filename || 'Choose file...'}
                            inputProps={{
                                type: 'file',
                                accept: '.nj, .qst, .xj',
                                onChange: this.onFileChange
                            }}
                        />
                        {areas ? (
                            <HTMLSelect
                                onChange={this.onAreaSelectChange}
                                defaultValue={areaId}
                            >
                                {areas.map(area =>
                                    <option key={area.id} value={area.id}>{area.name}</option>
                                )}
                            </HTMLSelect>
                        ) : null}
                        {quest ? (
                            <Button
                                text="Save as..."
                                icon="floppy-disk"
                                onClick={this.onSaveAsClick}
                            />
                        ) : null}
                    </NavbarGroup>
                </Navbar>
                <div className="QuestEditorComponent-main">
                    <QuestInfoComponent quest={quest} />
                    <RendererComponent
                        quest={quest}
                        area={area}
                        model={model} />
                    <EntityInfoComponent entity={questEditorStore.selectedEntity} />
                </div>
                <Dialog
                    title="Save as..."
                    icon="floppy-disk"
                    className={Classes.DARK}
                    style={{ width: 360 }}
                    isOpen={this.state.saveDialogOpen}
                    onClose={this.onSaveDialogClose}>
                    <div className={Classes.DIALOG_BODY}>
                        <FormGroup label="Name:" labelFor="file-name-input">
                            <InputGroup
                                id="file-name-input"
                                autoFocus={true}
                                value={this.state.saveDialogFilename}
                                maxLength={12}
                                onChange={this.onSaveDialogNameChange}
                                onKeyUp={this.onSaveDialogNameKeyUp}
                            />
                        </FormGroup>
                    </div>
                    <div className={Classes.DIALOG_FOOTER}>
                        <div className={Classes.DIALOG_FOOTER_ACTIONS}>
                            <Button
                                text="Save"
                                style={{ marginLeft: 10 }}
                                onClick={this.onSaveDialogSaveClick}
                                intent={Intent.PRIMARY} />
                        </div>
                    </div>
                </Dialog>
            </div>
        );
    }

    private onFileChange = (e: ChangeEvent<HTMLInputElement>) => {
        if (e.currentTarget.files) {
            const file = e.currentTarget.files[0];

            if (file) {
                this.setState({
                    filename: file.name
                });
                loadFile(file);
            }
        }
    }

    private onAreaSelectChange = (e: ChangeEvent<HTMLSelectElement>) => {
        const areaId = parseInt(e.currentTarget.value, 10);
        setCurrentAreaId(areaId);
    }

    private onSaveAsClick = () => {
        let name = this.state.filename || 'Untitled';
        name = name.endsWith('.qst') ? name.slice(0, -4) : name;

        this.setState({
            saveDialogOpen: true,
            saveDialogFilename: name
        });
    }

    private onSaveDialogNameChange = (e: ChangeEvent<HTMLInputElement>) => {
        this.setState({ saveDialogFilename: e.currentTarget.value });
    }

    private onSaveDialogNameKeyUp = (e: KeyboardEvent<HTMLInputElement>) => {
        if (e.key === 'Enter') {
            this.onSaveDialogSaveClick();
        }
    }

    private onSaveDialogSaveClick = () => {
        saveCurrentQuestToFile(this.state.saveDialogFilename);
        this.setState({ saveDialogOpen: false });
    }

    private onSaveDialogClose = () => {
        this.setState({ saveDialogOpen: false });
    }
}