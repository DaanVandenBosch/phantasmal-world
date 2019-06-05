import { Menu } from 'antd';
import { ClickParam } from 'antd/lib/menu';
import { observer } from 'mobx-react';
import React from 'react';
import './ApplicationComponent.css';
import { withErrorBoundary } from './ErrorBoundary';
import { HuntOptimizerComponent } from './hunt-optimizer/HuntOptimizerComponent';
import { QuestEditorComponent } from './quest-editor/QuestEditorComponent';

const QuestEditor = withErrorBoundary(QuestEditorComponent);
const HuntOptimizer = withErrorBoundary(HuntOptimizerComponent);

@observer
export class ApplicationComponent extends React.Component {
    state = { tool: 'huntOptimizer' }

    render() {
        let toolComponent;

        switch (this.state.tool) {
            case 'questEditor':
                toolComponent = <QuestEditor />;
                break;
            case 'huntOptimizer':
                toolComponent = <HuntOptimizer />;
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
                <div className="ApplicationComponent-main">
                    {toolComponent}
                </div>
            </div>
        );
    }

    private menuClicked = (e: ClickParam) => {
        this.setState({ tool: e.key });
    };
}
