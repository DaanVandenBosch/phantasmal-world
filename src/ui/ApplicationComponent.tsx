import { Button, Dialog, Intent } from '@blueprintjs/core';
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
        const areas = quest ? Array.from(quest.areaVariants).map(a => a.area) : undefined;
        const area = appStateStore.currentArea;
        const areaId = area ? String(area.id) : undefined;

        return (
            <div className="ApplicationComponent bp3-app bp3-dark">
                <nav className="bp3-navbar">
                    <div className="bp3-navbar-group">
                        <div className="ApplicationComponent-heading bp3-navbar-heading">
                            Phantasmal Quest Editor
                            <sup className="ApplicationComponent-beta">BETA</sup>
                        </div>
                        <label className="bp3-file-input">
                            <input
                                type="file"
                                accept=".nj, .qst, .xj"
                                onChange={this.onFileChange} />
                            <span className="bp3-file-upload-input">
                                <span className="ApplicationComponent-file-upload">
                                    {this.state.filename || 'Choose file...'}
                                </span>
                            </span>
                        </label>
                        {areas
                            ? (
                                <div className="bp3-select" style={{ marginLeft: 10 }}>
                                    <select
                                        onChange={this.onAreaSelectChange}
                                        defaultValue={areaId}>
                                        {areas.map(area =>
                                            <option key={area.id} value={area.id}>{area.name}</option>)}
                                    </select>
                                </div>
                            ) : null}
                        {quest
                            ? <Button
                                text="Save as..."
                                icon="floppy-disk"
                                style={{ marginLeft: 10 }}
                                onClick={this.onSaveAsClick} />
                            : null}
                    </div>
                </nav>
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
                    className="bp3-dark"
                    style={{ width: 360 }}
                    isOpen={this.state.saveDialogOpen}
                    onClose={this.onSaveDialogClose}>
                    <div className="bp3-dialog-body">
                        <label className="bp3-label bp3-inline">
                            Name:
                            <input
                                autoFocus={true}
                                className="bp3-input"
                                style={{ width: 200, margin: '0 10px 0 10px' }}
                                value={this.state.saveDialogFilename}
                                maxLength={12}
                                onChange={this.onSaveDialogNameChange}
                                onKeyUp={this.onSaveDialogNameKeyUp}
                            />
                            (.qst)
                        </label>
                    </div>
                    <div className="bp3-dialog-footer">
                        <div className="bp3-dialog-footer-actions">
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
