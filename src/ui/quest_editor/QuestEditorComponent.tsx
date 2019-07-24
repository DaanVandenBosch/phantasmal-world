import GoldenLayout from "golden-layout";
import Logger from "js-logger";
import { observer } from "mobx-react";
import React, { Component, createRef, FocusEvent, ReactNode } from "react";
import { quest_editor_ui_persister } from "../../persistence/QuestEditorUiPersister";
import { quest_editor_store } from "../../stores/QuestEditorStore";
import { EntityInfoComponent } from "./EntityInfoComponent";
import "./QuestEditorComponent.less";
import { QuestInfoComponent } from "./QuestInfoComponent";
import { QuestRendererComponent } from "./QuestRendererComponent";
import { ScriptEditorComponent } from "./ScriptEditorComponent";
import { Toolbar } from "./Toolbar";

const logger = Logger.get("ui/quest_editor/QuestEditorComponent");

const DEFAULT_LAYOUT_CONFIG = {
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
                    component: QuestInfoComponent.name,
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
                            component: QuestRendererComponent.name,
                            isClosable: false,
                        },
                        {
                            title: "Script",
                            type: "react-component",
                            component: ScriptEditorComponent.name,
                            isClosable: false,
                        },
                    ],
                },
                {
                    title: "Entity",
                    type: "react-component",
                    component: EntityInfoComponent.name,
                    isClosable: false,
                    width: 2,
                },
            ],
        },
    ],
};

@observer
export class QuestEditorComponent extends Component {
    private layout_element = createRef<HTMLDivElement>();
    private layout?: GoldenLayout;

    componentDidMount(): void {
        quest_editor_store.undo.make_current();

        window.addEventListener("resize", this.resize);

        setTimeout(async () => {
            if (this.layout_element.current && !this.layout) {
                const config = await quest_editor_ui_persister.load_layout_config(
                    [
                        QuestInfoComponent.name,
                        QuestRendererComponent.name,
                        EntityInfoComponent.name,
                        ScriptEditorComponent.name,
                    ],
                    DEFAULT_LAYOUT_CONFIG
                );

                try {
                    this.layout = new GoldenLayout(config, this.layout_element.current);
                } catch (e) {
                    logger.warn("Couldn't initialize golden layout with persisted layout.", e);

                    this.layout = new GoldenLayout(
                        DEFAULT_LAYOUT_CONFIG,
                        this.layout_element.current
                    );
                }

                this.layout.registerComponent(QuestInfoComponent.name, QuestInfoComponent);
                this.layout.registerComponent(QuestRendererComponent.name, QuestRendererComponent);
                this.layout.registerComponent(EntityInfoComponent.name, EntityInfoComponent);
                this.layout.registerComponent(ScriptEditorComponent.name, ScriptEditorComponent);
                this.layout.on("stateChanged", () => {
                    if (this.layout) {
                        quest_editor_ui_persister.persist_layout_config(this.layout.toConfig());
                    }
                });

                this.layout.init();
            }
        }, 0);
    }

    componentWillUnmount(): void {
        quest_editor_store.undo.ensure_not_current();

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
                <div
                    className="qe-QuestEditorComponent-main"
                    onFocus={this.focus}
                    ref={this.layout_element}
                />
            </div>
        );
    }

    private focus = (e: FocusEvent) => {
        const scrip_editor_element = document.getElementById("qe-ScriptEditorComponent");

        if (
            scrip_editor_element &&
            scrip_editor_element.compareDocumentPosition(e.target) &
                Node.DOCUMENT_POSITION_CONTAINED_BY
        ) {
            quest_editor_store.script_undo.make_current();
        } else {
            quest_editor_store.undo.make_current();
        }
    };

    private resize = () => {
        if (this.layout) {
            this.layout.updateSize();
        }
    };
}
