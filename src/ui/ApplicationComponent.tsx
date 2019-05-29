import { Classes, Navbar, NavbarGroup, NavbarHeading, Button } from '@blueprintjs/core';
import { observer } from 'mobx-react';
import React from 'react';
import './ApplicationComponent.css';
import { QuestEditorComponent } from './quest-editor/QuestEditorComponent';
import { observable, action } from 'mobx';

@observer
export class ApplicationComponent extends React.Component {
    @observable private tool = 'quest-editor';

    render() {
        let toolComponent;

        switch (this.tool) {
            case 'quest-editor':
                toolComponent = <QuestEditorComponent />;
                break;
        }

        return (
            <div className={`ApplicationComponent ${Classes.DARK}`}>
                <Navbar>
                    <NavbarGroup className="ApplicationComponent-button-bar">
                        <NavbarHeading className="ApplicationComponent-heading">
                            Phantasmal World
                        </NavbarHeading>
                        <Button
                            text="Quest Editor (Beta)"
                            minimal={true}
                            onClick={() => this.setTool('quest-editor')}
                        />
                    </NavbarGroup>
                </Navbar>
                <div className="ApplicationComponent-main">
                    {toolComponent}
                </div>
            </div>
        );
    }

    private setTool = action('setTool', (tool: string) => {
        this.tool = tool;
    });
}
