import { Classes, Navbar, NavbarGroup, NavbarHeading, Button, Callout, Intent } from '@blueprintjs/core';
import { observer } from 'mobx-react';
import React from 'react';
import { QuestEditorComponent } from './quest-editor/QuestEditorComponent';
import { observable, action } from 'mobx';
import { HuntOptimizerComponent } from './hunt-optimizer/HuntOptimizerComponent';
import './ApplicationComponent.css';

@observer
export class ApplicationComponent extends React.Component {
    @observable private tool = 'quest-editor';

    render() {
        let toolComponent;

        switch (this.tool) {
            case 'quest-editor':
                toolComponent = <QuestEditorComponent />;
                break;
            case 'hunt-optimizer':
                toolComponent = <HuntOptimizerComponent />;
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
                            active={this.tool === 'quest-editor'}
                            onClick={() => this.setTool('quest-editor')}
                        />
                        <Button
                            text="Hunt Optimizer"
                            minimal={true}
                            active={this.tool === 'hunt-optimizer'}
                            onClick={() => this.setTool('hunt-optimizer')}
                        />
                    </NavbarGroup>
                </Navbar>
                <ErrorBoundary>
                    {toolComponent}
                </ErrorBoundary>
            </div>
        );
    }

    private setTool = action('setTool', (tool: string) => {
        this.tool = tool;
    });
}

class ErrorBoundary extends React.Component {
    state = {
        hasError: false
    }

    render() {
        return (
            <div className="ApplicationComponent-main" >
                {this.state.hasError ? (
                    <div className="ApplicationComponent-error">
                        <div>
                            <Callout intent={Intent.DANGER} title="Something went wrong." />
                        </div>
                    </div>
                ) : this.props.children}
            </div>
        );
    }

    static getDerivedStateFromError(_error: Error) {
        return { hasError: true };
    }
}
