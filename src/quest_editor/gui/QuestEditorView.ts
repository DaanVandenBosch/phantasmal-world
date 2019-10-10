import { ResizableWidget } from "../../core/gui/ResizableWidget";
import { create_element, el } from "../../core/gui/dom";
import { QuestEditorToolBar } from "./QuestEditorToolBar";
import GoldenLayout, { Container, ContentItem, ItemConfigType } from "golden-layout";
import { quest_editor_ui_persister } from "../persistence/QuestEditorUiPersister";
import { QuestInfoView } from "./QuestInfoView";
import "golden-layout/src/css/goldenlayout-base.css";
import "../../core/gui/golden_layout_theme.css";
import { NpcCountsView } from "./NpcCountsView";
import { QuestRendererView } from "./QuestRendererView";
import { AsmEditorView } from "./AsmEditorView";
import { EntityInfoView } from "./EntityInfoView";
import { gui_store, GuiTool } from "../../core/stores/GuiStore";
import { quest_editor_store } from "../stores/QuestEditorStore";
import { NpcListView } from "./NpcListView";
import { ObjectListView } from "./ObjectListView";
import { EventsView } from "./EventsView";
import Logger = require("js-logger");

const logger = Logger.get("quest_editor/gui/QuestEditorView");

// Don't change these values, as they are persisted in the user's browser.
const VIEW_TO_NAME = new Map<new () => ResizableWidget, string>([
    [QuestInfoView, "quest_info"],
    [NpcCountsView, "npc_counts"],
    [QuestRendererView, "quest_renderer"],
    [AsmEditorView, "asm_editor"],
    [EntityInfoView, "entity_info"],
    [NpcListView, "npc_list_view"],
    [ObjectListView, "object_list_view"],
]);

if (gui_store.feature_active("events")) {
    VIEW_TO_NAME.set(EventsView, "events_view");
}

const DEFAULT_LAYOUT_CONFIG = {
    settings: {
        showPopoutIcon: false,
        showMaximiseIcon: false,
    },
    dimensions: {
        headerHeight: 24,
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
                type: "column",
                width: 2,
                content: [
                    {
                        type: "stack",
                        content: [
                            {
                                title: "Info",
                                type: "component",
                                componentName: VIEW_TO_NAME.get(QuestInfoView),
                                isClosable: false,
                            },
                            {
                                title: "NPC Counts",
                                type: "component",
                                componentName: VIEW_TO_NAME.get(NpcCountsView),
                                isClosable: false,
                            },
                        ],
                    },
                    {
                        title: "Entity",
                        type: "component",
                        componentName: VIEW_TO_NAME.get(EntityInfoView),
                        isClosable: false,
                    },
                ],
            },
            {
                type: "stack",
                width: 9,
                content: [
                    {
                        title: "3D View",
                        type: "component",
                        componentName: VIEW_TO_NAME.get(QuestRendererView),
                        isClosable: false,
                    },
                    {
                        title: "Script",
                        type: "component",
                        componentName: VIEW_TO_NAME.get(AsmEditorView),
                        isClosable: false,
                    },
                ],
            },
            {
                type: "stack",
                width: 2,
                content: [
                    {
                        title: "NPCs",
                        type: "component",
                        componentName: VIEW_TO_NAME.get(NpcListView),
                        isClosable: false,
                    },
                    {
                        title: "Objects",
                        type: "component",
                        componentName: VIEW_TO_NAME.get(ObjectListView),
                        isClosable: false,
                    },
                    ...(gui_store.feature_active("events")
                        ? [
                              {
                                  title: "Events",
                                  type: "component",
                                  componentName: VIEW_TO_NAME.get(EventsView),
                                  isClosable: false,
                              },
                          ]
                        : []),
                ],
            },
        ],
    },
];

export class QuestEditorView extends ResizableWidget {
    readonly element = el.div({ class: "quest_editor_QuestEditorView" });

    private readonly tool_bar_view = this.disposable(new QuestEditorToolBar());

    private readonly layout_element = create_element("div", { class: "quest_editor_gl_container" });
    private readonly layout: Promise<GoldenLayout>;

    private readonly sub_views = new Map<string, ResizableWidget>();

    constructor() {
        super();

        this.element.append(this.tool_bar_view.element, this.layout_element);

        this.layout = this.init_golden_layout();

        this.disposables(
            gui_store.on_global_keydown(
                GuiTool.QuestEditor,
                "Ctrl-Alt-D",
                () => (quest_editor_store.debug.val = !quest_editor_store.debug.val),
            ),
        );

        this.finalize_construction(QuestEditorView.prototype);
    }

    resize(width: number, height: number): this {
        super.resize(width, height);

        const layout_height = Math.max(0, height - this.tool_bar_view.height);
        this.layout_element.style.width = `${width}px`;
        this.layout_element.style.height = `${layout_height}px`;
        this.layout.then(layout => layout.updateSize(width, layout_height));

        return this;
    }

    dispose(): void {
        super.dispose();
        this.layout.then(layout => layout.destroy());

        for (const view of this.sub_views.values()) {
            view.dispose();
        }

        this.sub_views.clear();
    }

    private async init_golden_layout(): Promise<GoldenLayout> {
        const content = await quest_editor_ui_persister.load_layout_config(
            [...VIEW_TO_NAME.values()],
            DEFAULT_LAYOUT_CONTENT,
        );

        try {
            return this.attempt_gl_init({
                ...DEFAULT_LAYOUT_CONFIG,
                content,
            });
        } catch (e) {
            logger.warn("Couldn't instantiate golden layout with persisted layout.", e);

            return this.attempt_gl_init({
                ...DEFAULT_LAYOUT_CONFIG,
                content: DEFAULT_LAYOUT_CONTENT,
            });
        }
    }

    private attempt_gl_init(config: GoldenLayout.Config): GoldenLayout {
        const layout = new GoldenLayout(config, this.layout_element);
        // eslint-disable-next-line @typescript-eslint/no-this-alias
        const self = this;

        try {
            for (const [view_ctor, name] of VIEW_TO_NAME) {
                // registerComponent expects a regular function and not an arrow function.
                // This function will be called with new.
                layout.registerComponent(name, function(container: Container) {
                    const view = new view_ctor();

                    container.on("close", () => view.dispose());
                    container.on("resize", () =>
                        // Subtract 4 from height to work around bug in Golden Layout related to headerHeight.
                        view.resize(container.width, container.height - 4),
                    );

                    view.resize(container.width, container.height);

                    self.sub_views.set(name, view);
                    container.getElement().append(view.element);
                });
            }

            layout.on("stateChanged", () => {
                if (this.layout) {
                    quest_editor_ui_persister.persist_layout_config(layout.toConfig().content);
                }
            });

            layout.on("stackCreated", (stack: ContentItem) => {
                stack.on("activeContentItemChanged", (item: ContentItem) => {
                    if ("componentName" in item.config) {
                        const view = this.sub_views.get(item.config.componentName);
                        if (view) view.focus();
                    }
                });
            });

            layout.init();

            return layout;
        } catch (e) {
            layout.destroy();
            throw e;
        }
    }
}
