import { Menu, Select } from 'antd';
import { ClickParam } from 'antd/lib/menu';
import { observer } from 'mobx-react';
import React from 'react';
import './ApplicationComponent.less';
import { withErrorBoundary } from './ErrorBoundary';
import { HuntOptimizerComponent } from './hunt-optimizer/HuntOptimizerComponent';
import { QuestEditorComponent } from './quest-editor/QuestEditorComponent';
import { DpsCalcComponent } from './dps-calc/DpsCalcComponent';
import { Server } from '../domain';

const QuestEditor = withErrorBoundary(QuestEditorComponent);
const HuntOptimizer = withErrorBoundary(HuntOptimizerComponent);
const DpsCalc = withErrorBoundary(DpsCalcComponent);

@observer
export class ApplicationComponent extends React.Component {
    state = { tool: this.initTool() }

    render() {
        let toolComponent;

        switch (this.state.tool) {
            case 'questEditor':
                toolComponent = <QuestEditor />;
                break;
            case 'huntOptimizer':
                toolComponent = <HuntOptimizer />;
                break;
            case 'dpsCalc':
                toolComponent = <DpsCalc />;
                break;
        }

        return (
            <div className="ApplicationComponent">
                <div className="ApplicationComponent-navbar">
                    <h1 className="ApplicationComponent-heading">
                        Phantasmal World
                    </h1>
                    <Menu
                        className="ApplicationComponent-heading-menu"
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
                        {/* <Menu.Item key="dpsCalc">
                            DPS Calculator
                        </Menu.Item> */}
                    </Menu>
                    <div className="ApplicationComponent-server-select">
                        <span>Server:</span>
                        <Select defaultValue={Server.Ephinea} style={{ width: 120 }}>
                            <Select.Option value={Server.Ephinea}>{Server.Ephinea}</Select.Option>
                        </Select>
                    </div>
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

    private initTool(): string {
        const param = window.location.search.slice(1).split('&').find(p => p.startsWith('tool='));
        return param ? param.slice(5) : 'questEditor';
    }
}
