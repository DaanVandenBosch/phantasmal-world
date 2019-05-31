import { Alert, Menu } from 'antd';
import { ClickParam } from 'antd/lib/menu';
import { observer } from 'mobx-react';
import React from 'react';
import './ApplicationComponent.css';
import { HuntOptimizerComponent } from './hunt-optimizer/HuntOptimizerComponent';
import { QuestEditorComponent } from './quest-editor/QuestEditorComponent';

@observer
export class ApplicationComponent extends React.Component {
    state = { tool: 'huntOptimizer' }

    render() {
        let toolComponent;

        switch (this.state.tool) {
            case 'questEditor':
                toolComponent = <QuestEditorComponent />;
                break;
            case 'huntOptimizer':
                toolComponent = <HuntOptimizerComponent />;
                break;
        }

        return (
            <div className="ApplicationComponent">
                <div className="ApplicationComponent-navbar">
                    <h1 className="ApplicationComponent-heading">
                        Phantasmal World
                    </h1>
                    <Menu
                        onClick={this.menuClicked}
                        selectedKeys={[this.state.tool]}
                        mode="horizontal"
                    >
                        <Menu.Item key="questEditor">
                            Quest Editor<sup className="ApplicationComponent-beta">(Beta)</sup>
                        </Menu.Item>
                        <Menu.Item key="huntOptimizer">
                            Hunt Optimizer
                            </Menu.Item>
                    </Menu>
                </div>
                <ErrorBoundary>
                    {toolComponent}
                </ErrorBoundary>
            </div>
        );
    }

    private menuClicked = (e: ClickParam) => {
        this.setState({ tool: e.key });
    };
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
                            <Alert type="error" message="Something went wrong." />
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
