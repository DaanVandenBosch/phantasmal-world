import { Menu, Select } from "antd";
import { ClickParam } from "antd/lib/menu";
import { observer } from "mobx-react";
import React, { ReactNode, Component } from "react";
import { Server } from "../domain";
import styles from "./ApplicationComponent.css";
import { DpsCalcComponent } from "./dps_calc/DpsCalcComponent";
import { with_error_boundary } from "./ErrorBoundary";
import { HuntOptimizerComponent } from "./hunt_optimizer/HuntOptimizerComponent";
import { QuestEditorComponent } from "./quest_editor/QuestEditorComponent";
import { ViewerComponent } from "./viewer/ViewerComponent";
import { application_store } from "../stores/ApplicationStore";

const Viewer = with_error_boundary(ViewerComponent);
const QuestEditor = with_error_boundary(QuestEditorComponent);
const HuntOptimizer = with_error_boundary(HuntOptimizerComponent);
const DpsCalc = with_error_boundary(DpsCalcComponent);

@observer
export class ApplicationComponent extends Component {
    componentDidMount(): void {
        window.addEventListener("keyup", this.keyup);
    }

    componentWillUnmount(): void {
        window.removeEventListener("keyup", this.keyup);
    }

    render(): ReactNode {
        let tool_component;

        switch (application_store.current_tool) {
            case "viewer":
                tool_component = <Viewer />;
                break;
            case "quest_editor":
                tool_component = <QuestEditor />;
                break;
            case "hunt_optimizer":
                tool_component = <HuntOptimizer />;
                break;
            case "dps_calc":
                tool_component = <DpsCalc />;
                break;
        }

        return (
            <div className={styles.main}>
                <div className={styles.navbar}>
                    <Menu
                        className={styles.heading_menu}
                        onClick={this.menu_clicked}
                        selectedKeys={[application_store.current_tool]}
                        mode="horizontal"
                    >
                        <Menu.Item key="viewer">
                            Viewer<sup className={styles.beta}>(Beta)</sup>
                        </Menu.Item>
                        <Menu.Item key="quest_editor">
                            Quest Editor<sup className={styles.beta}>(Beta)</sup>
                        </Menu.Item>
                        <Menu.Item key="hunt_optimizer">Hunt Optimizer</Menu.Item>
                        {/* <Menu.Item key="dpsCalc">
                            DPS Calculator
                        </Menu.Item> */}
                    </Menu>
                    <div className={styles.server_select}>
                        <span>Server:</span>
                        <Select defaultValue={Server.Ephinea} style={{ width: 120 }}>
                            <Select.Option value={Server.Ephinea}>{Server.Ephinea}</Select.Option>
                        </Select>
                    </div>
                </div>
                <div className={styles.content}>{tool_component}</div>
            </div>
        );
    }

    private menu_clicked = (e: ClickParam) => {
        application_store.current_tool = e.key;
    };

    private keyup = (e: KeyboardEvent) => {
        application_store.dispatch_global_keyup(e);
    };
}
