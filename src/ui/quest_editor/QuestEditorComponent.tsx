import { observer } from "mobx-react";
import React, { Component, ReactNode } from "react";
import { get_quest_renderer } from "../../rendering/QuestRenderer";
import { application_store } from "../../stores/ApplicationStore";
import { quest_editor_store } from "../../stores/QuestEditorStore";
import { RendererComponent } from "../RendererComponent";
import { EntityInfoComponent } from "./EntityInfoComponent";
import "./QuestEditorComponent.less";
import { QuestInfoComponent } from "./QuestInfoComponent";
import { Toolbar } from "./Toolbar";
import { Tabs } from "antd";
import { ScriptEditorComponent } from "./ScriptEditorComponent";
import { AutoSizer } from "react-virtualized";

@observer
export class QuestEditorComponent extends Component<{}, { debug: boolean }> {
    state = { debug: false };

    componentDidMount(): void {
        application_store.on_global_keyup("quest_editor", this.keyup);
    }

    render(): ReactNode {
        const quest = quest_editor_store.current_quest;

        return (
            <div className="qe-QuestEditorComponent">
                <Toolbar />
                <div className="qe-QuestEditorComponent-main">
                    <QuestInfoComponent quest={quest} />
                    <Tabs type="card" className="qe-QuestEditorComponent-tabcontainer">
                        <Tabs.TabPane
                            tab="Entities"
                            key="entities"
                            className="qe-QuestEditorComponent-tab"
                        >
                            <div className="qe-QuestEditorComponent-tab-main">
                                <AutoSizer>
                                    {({ width, height }) => (
                                        <RendererComponent
                                            renderer={get_quest_renderer()}
                                            width={width}
                                            height={height}
                                            debug={this.state.debug}
                                        />
                                    )}
                                </AutoSizer>
                            </div>
                            <EntityInfoComponent entity={quest_editor_store.selected_entity} />
                        </Tabs.TabPane>
                        <Tabs.TabPane
                            tab="Script"
                            key="script"
                            className="qe-QuestEditorComponent-tab"
                        >
                            <ScriptEditorComponent className="qe-QuestEditorComponent-tab-main" />
                        </Tabs.TabPane>
                    </Tabs>
                </div>
            </div>
        );
    }

    private keyup = (e: KeyboardEvent) => {
        if (e.ctrlKey && e.key === "z" && !e.altKey) {
            quest_editor_store.undo_stack.undo();
        } else if (e.ctrlKey && e.key === "Z" && !e.altKey) {
            quest_editor_store.undo_stack.redo();
        } else if (e.ctrlKey && e.altKey && e.key === "d") {
            this.setState(state => ({ debug: !state.debug }));
        }
    };
}
