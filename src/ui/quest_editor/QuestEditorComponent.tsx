import { observer } from "mobx-react";
import React, { Component, ReactNode } from "react";
import { get_quest_renderer } from "../../rendering/QuestRenderer";
import { application_store } from "../../stores/ApplicationStore";
import { quest_editor_store } from "../../stores/QuestEditorStore";
import { RendererComponent } from "../RendererComponent";
import { EntityInfoComponent } from "./EntityInfoComponent";
import "./QuestEditorComponent.css";
import { QuestInfoComponent } from "./QuestInfoComponent";
import { Toolbar } from "./Toolbar";

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
                    <RendererComponent renderer={get_quest_renderer()} debug={this.state.debug} />
                    <EntityInfoComponent entity={quest_editor_store.selected_entity} />
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
