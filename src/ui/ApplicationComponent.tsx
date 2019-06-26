import { Menu, Select } from 'antd';
import { ClickParam } from 'antd/lib/menu';
import { observer } from 'mobx-react';
import React from 'react';
import './ApplicationComponent.less';
import { with_error_boundary } from './ErrorBoundary';
import { HuntOptimizerComponent } from './hunt_optimizer/HuntOptimizerComponent';
import { QuestEditorComponent } from './quest_editor/QuestEditorComponent';
import { DpsCalcComponent } from './dps_calc/DpsCalcComponent';
import { Server } from '../domain';

const QuestEditor = with_error_boundary(QuestEditorComponent);
const HuntOptimizer = with_error_boundary(HuntOptimizerComponent);
const DpsCalc = with_error_boundary(DpsCalcComponent);

@observer
export class ApplicationComponent extends React.Component {
    state = { tool: this.init_tool() }

    render() {
        let tool_component;

        switch (this.state.tool) {
            case 'questEditor':
                tool_component = <QuestEditor />;
                break;
            case 'huntOptimizer':
                tool_component = <HuntOptimizer />;
                break;
            case 'dpsCalc':
                tool_component = <DpsCalc />;
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
                        onClick={this.menu_clicked}
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
                    {tool_component}
                </div>
            </div>
        );
    }

    private menu_clicked = (e: ClickParam) => {
        this.setState({ tool: e.key });
    };

    private init_tool(): string {
        const param = window.location.search.slice(1).split('&').find(p => p.startsWith('tool='));
        return param ? param.slice(5) : 'questEditor';
    }
}
