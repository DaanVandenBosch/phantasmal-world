import GoldenLayout from "golden-layout";
import { observer } from "mobx-react";
import React, { Component, createRef, ReactNode } from "react";
import { application_store } from "../../stores/ApplicationStore";
import { quest_editor_store } from "../../stores/QuestEditorStore";
import { EntityInfoComponent } from "./EntityInfoComponent";
import "./QuestEditorComponent.less";
import { QuestInfoComponent } from "./QuestInfoComponent";
import { QuestRendererComponent } from "./QuestRendererComponent";
import { ScriptEditorComponent } from "./ScriptEditorComponent";
import { Toolbar } from "./Toolbar";

@observer
export class QuestEditorComponent extends Component {
    private layout_element = createRef<HTMLDivElement>();
    private layout?: GoldenLayout;

    componentDidMount(): void {
        application_store.on_global_keyup("quest_editor", this.keyup);

        window.addEventListener("resize", this.resize);

        setTimeout(() => {
            if (this.layout_element.current && !this.layout) {
                this.layout = new GoldenLayout(
                    {
                        settings: {
                            showPopoutIcon: false,
                        },
                        dimensions: {
                            headerHeight: 28,
                        },
                        labels: {
                            close: "Close",
                            maximise: "Maximise",
                            minimise: "Minimise",
                            popout: "Open in new window",
                        },
                        content: [
                            {
                                type: "row",
                                content: [
                                    {
                                        title: "Info",
                                        type: "react-component",
                                        component: "QuestInfoComponent",
                                        isClosable: false,
                                        width: 3,
                                    },
                                    {
                                        type: "stack",
                                        width: 9,
                                        content: [
                                            {
                                                title: "3D View",
                                                type: "react-component",
                                                component: "QuestRendererComponent",
                                                isClosable: false,
                                            },
                                            {
                                                title: "Script",
                                                type: "react-component",
                                                component: "ScriptEditorComponent",
                                                isClosable: false,
                                            },
                                        ],
                                    },
                                    {
                                        title: "Entity",
                                        type: "react-component",
                                        component: "EntityInfoComponent",
                                        isClosable: false,
                                        width: 2,
                                    },
                                ],
                            },
                        ],
                    },
                    this.layout_element.current
                );
                this.layout.registerComponent("QuestInfoComponent", QuestInfoComponent);
                this.layout.registerComponent("QuestRendererComponent", QuestRendererComponent);
                this.layout.registerComponent("EntityInfoComponent", EntityInfoComponent);
                this.layout.registerComponent("ScriptEditorComponent", ScriptEditorComponent);
                this.layout.init();
            }
        }, 0);
    }

    componentWillUnmount(): void {
        window.removeEventListener("resize", this.resize);

        if (this.layout) {
            this.layout.destroy();
            this.layout = undefined;
        }
    }

    render(): ReactNode {
        return (
            <div className="qe-QuestEditorComponent">
                <Toolbar />
                <div className="qe-QuestEditorComponent-main" ref={this.layout_element} />
            </div>
        );
    }

    private keyup = (e: KeyboardEvent) => {
        if (e.ctrlKey && e.key === "z" && !e.altKey) {
            quest_editor_store.undo_stack.undo();
        } else if (e.ctrlKey && e.key === "Z" && !e.altKey) {
            quest_editor_store.undo_stack.redo();
        } else if (e.ctrlKey && e.altKey && e.key === "d") {
            quest_editor_store.toggle_debug();
        }
    };

    private resize = () => {
        if (this.layout) {
            this.layout.updateSize();
        }
    };
}
