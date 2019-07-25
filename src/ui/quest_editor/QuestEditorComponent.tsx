import GoldenLayout, { ItemConfigType, ContentItem } from "golden-layout";
import Logger from "js-logger";
import { observer } from "mobx-react";
import React, { Component, createRef, FocusEvent, ReactNode } from "react";
import { quest_editor_ui_persister } from "../../persistence/QuestEditorUiPersister";
import { quest_editor_store } from "../../stores/QuestEditorStore";
import { EntityInfoComponent } from "./EntityInfoComponent";
import "./QuestEditorComponent.less";
import { QuestInfoComponent } from "./QuestInfoComponent";
import { QuestRendererComponent } from "./QuestRendererComponent";
import { AssemblyEditorComponent } from "./AssemblyEditorComponent";
import { Toolbar } from "./Toolbar";

const logger = Logger.get("ui/quest_editor/QuestEditorComponent");

// Don't change these ids, as they are persisted in the user's browser.
const CMP_TO_NAME = new Map([
    [QuestInfoComponent, "quest_info"],
    [QuestRendererComponent, "quest_renderer"],
    [AssemblyEditorComponent, "assembly_editor"],
    [EntityInfoComponent, "entity_info"],
]);

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
};

const DEFAULT_LAYOUT_CONTENT: ItemConfigType[] = [
    {
        type: "row",
        content: [
            {
                title: "Info",
                type: "react-component",
                component: CMP_TO_NAME.get(QuestInfoComponent),
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
                        component: CMP_TO_NAME.get(QuestRendererComponent),
                        isClosable: false,
                    },
                    {
                        title: "Script",
                        type: "react-component",
                        component: CMP_TO_NAME.get(AssemblyEditorComponent),
                        isClosable: false,
                    },
                ],
            },
            {
                title: "Entity",
                type: "react-component",
                component: CMP_TO_NAME.get(EntityInfoComponent),
                isClosable: false,
                width: 2,
            },
        ],
    },
];

@observer
export class QuestEditorComponent extends Component {
    private layout_element = createRef<HTMLDivElement>();
    private layout?: GoldenLayout;

    componentDidMount(): void {
        quest_editor_store.undo.make_current();

        window.addEventListener("resize", this.resize);

        setTimeout(async () => {
            if (this.layout_element.current && !this.layout) {
                const content = await quest_editor_ui_persister.load_layout_config(
                    [...CMP_TO_NAME.values()],
                    DEFAULT_LAYOUT_CONTENT
                );

                const config: GoldenLayout.Config = {
                    ...DEFAULT_LAYOUT_CONFIG,
                    content,
                };

                try {
                    this.layout = new GoldenLayout(config, this.layout_element.current);
                } catch (e) {
                    logger.warn("Couldn't initialize golden layout with persisted layout.", e);

                    this.layout = new GoldenLayout(
                        {
                            ...DEFAULT_LAYOUT_CONFIG,
                            content: DEFAULT_LAYOUT_CONTENT,
                        },
                        this.layout_element.current
                    );
                }

                for (const [component, name] of CMP_TO_NAME) {
                    this.layout.registerComponent(name, component);
                }

                this.layout.on("stateChanged", () => {
                    if (this.layout) {
                        quest_editor_ui_persister.persist_layout_config(
                            this.layout.toConfig().content
                        );
                    }
                });

                this.layout.on("stackCreated", (stack: ContentItem) => {
                    stack.on("activeContentItemChanged", (item: ContentItem) => {
                        if ("component" in item.config) {
                            if (
                                item.config.component === CMP_TO_NAME.get(AssemblyEditorComponent)
                            ) {
                                quest_editor_store.script_undo.make_current();
                            } else {
                                quest_editor_store.undo.make_current();
                            }
                        }
                    });
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
