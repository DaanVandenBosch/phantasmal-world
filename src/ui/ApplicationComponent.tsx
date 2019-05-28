import React, { ChangeEvent, KeyboardEvent } from 'react';
import { observer } from 'mobx-react';
import { Button, Dialog, Intent } from '@blueprintjs/core';
import { application_state } from '../store';
import { current_area_id_changed, load_file, save_current_quest_to_file } from '../actions';
import { Area3DComponent } from './Area3DComponent';
import { EntityInfoComponent } from './EntityInfoComponent';
import { QuestInfoComponent } from './QuestInfoComponent';
import './ApplicationComponent.css';

@observer
export class ApplicationComponent extends React.Component<{}, {
    filename?: string,
    save_dialog_open: boolean,
    save_dialog_filename: string
}> {
    state = {
        filename: undefined,
        save_dialog_open: false,
        save_dialog_filename: 'Untitled'
    };

    render() {
        const quest = application_state.current_quest;
        const model = application_state.current_model;
        const areas = quest ? Array.from(quest.area_variants).map(a => a.area) : undefined;
        const area = application_state.current_area;
        const area_id = area ? String(area.id) : undefined;

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
                                onChange={this._on_file_change} />
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
                                        onChange={this._on_area_select_change}
                                        defaultValue={area_id}>
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
                                onClick={this._on_save_as_click} />
                            : null}
                    </div>
                </nav>
                <div className="ApplicationComponent-main">
                    <QuestInfoComponent
                        quest={quest} />
                    <Area3DComponent
                        quest={quest}
                        area={area}
                        model={model} />
                    <EntityInfoComponent entity={application_state.selected_entity} />
                </div>
                <Dialog
                    title="Save as..."
                    icon="floppy-disk"
                    className="bp3-dark"
                    style={{ width: 360 }}
                    isOpen={this.state.save_dialog_open}
                    onClose={this._on_save_dialog_close}>
                    <div className="bp3-dialog-body">
                        <label className="bp3-label bp3-inline">
                            Name:
                            <input
                                autoFocus={true}
                                className="bp3-input"
                                style={{ width: 200, margin: '0 10px 0 10px' }}
                                value={this.state.save_dialog_filename}
                                onChange={this._on_save_dialog_name_change}
                                onKeyUp={this._on_save_dialog_name_key_up} />
                            (.qst)
                        </label>
                    </div>
                    <div className="bp3-dialog-footer">
                        <div className="bp3-dialog-footer-actions">
                            <Button
                                text="Save"
                                style={{ marginLeft: 10 }}
                                onClick={this._on_save_dialog_save_click}
                                intent={Intent.PRIMARY} />
                        </div>
                    </div>
                </Dialog>
            </div>
        );
    }

    private _on_file_change = (e: ChangeEvent<HTMLInputElement>) => {
        if (e.currentTarget.files) {
            const file = e.currentTarget.files[0];

            if (file) {
                this.setState({
                    filename: file.name
                });
                load_file(file);
            }
        }
    }

    private _on_area_select_change = (e: ChangeEvent<HTMLSelectElement>) => {
        const area_id = parseInt(e.currentTarget.value, 10);
        current_area_id_changed(area_id);
    }

    private _on_save_as_click = () => {
        let name = this.state.filename || 'Untitled';
        name = name.endsWith('.qst') ? name.slice(0, -4) : name;

        this.setState({
            save_dialog_open: true,
            save_dialog_filename: name
        });
    }

    private _on_save_dialog_name_change = (e: ChangeEvent<HTMLInputElement>) => {
        this.setState({ save_dialog_filename: e.currentTarget.value });
    }

    private _on_save_dialog_name_key_up = (e: KeyboardEvent<HTMLInputElement>) => {
        if (e.key === 'Enter') {
            this._on_save_dialog_save_click();
        }
    }

    private _on_save_dialog_save_click = () => {
        save_current_quest_to_file(this.state.save_dialog_filename);
        this.setState({ save_dialog_open: false });
    }

    private _on_save_dialog_close = () => {
        this.setState({ save_dialog_open: false });
    }
}
