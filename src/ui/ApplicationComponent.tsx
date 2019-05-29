import { Button, Dialog, Intent, Classes, Navbar, NavbarGroup, NavbarHeading, FileInput, HTMLSelect, FormGroup, InputGroup } from '@blueprintjs/core';
import { observer } from 'mobx-react';
import React, { ChangeEvent, KeyboardEvent } from 'react';
import { saveCurrentQuestToFile, setCurrentAreaId } from '../actions/appState';
import { loadFile } from '../actions/loadFile';
import { appStateStore } from '../stores/AppStateStore';
import './ApplicationComponent.css';
import { RendererComponent } from './RendererComponent';
import { EntityInfoComponent } from './EntityInfoComponent';
import { QuestInfoComponent } from './QuestInfoComponent';

@observer
export class ApplicationComponent extends React.Component<{}, {
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
        const quest = appStateStore.currentQuest;
        const model = appStateStore.currentModel;
        const areas = quest && Array.from(quest.areaVariants).map(a => a.area);
        const area = appStateStore.currentArea;
        const areaId = area && String(area.id);

        return (
            <div className={`ApplicationComponent ${Classes.DARK}`}>
                <Navbar>
                    <NavbarGroup className="ApplicationComponent-button-bar">
                        <NavbarHeading className="ApplicationComponent-heading">
                            Phantasmal World
                            <sup className="ApplicationComponent-beta">BETA</sup>
                        </NavbarHeading>
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
                <div className="ApplicationComponent-main">
                    <QuestInfoComponent
                        quest={quest} />
                    <RendererComponent
                        quest={quest}
                        area={area}
                        model={model} />
                    <EntityInfoComponent entity={appStateStore.selectedEntity} />
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
