import { ResizableWidget } from "../../core/gui/ResizableWidget";
import { create_element, el } from "../../core/gui/dom";
import { QuestEditorToolBar } from "./QuestEditorToolBar";
import GoldenLayout, { Container, ContentItem, ItemConfigType } from "golden-layout";
import { QuestInfoView } from "./QuestInfoView";
import "golden-layout/src/css/goldenlayout-base.css";
import "../../core/gui/golden_layout_theme.css";
import { NpcCountsView } from "./NpcCountsView";
import { QuestEditorRendererView } from "./QuestEditorRendererView";
import { AsmEditorView } from "./AsmEditorView";
import { EntityInfoView } from "./EntityInfoView";
import { GuiStore, GuiTool } from "../../core/stores/GuiStore";
import { NpcListView } from "./NpcListView";
import { ObjectListView } from "./ObjectListView";
import { EventsView } from "./EventsView";
import { RegistersView } from "./RegistersView";
import { LogView } from "./LogView";
import { QuestRunnerRendererView } from "./QuestRunnerRendererView";
import { QuestEditorStore } from "../stores/QuestEditorStore";
import { QuestEditorUiPersister } from "../persistence/QuestEditorUiPersister";
import { LogManager } from "../../core/Logger";
import { ErrorView } from "../../core/gui/ErrorView";

const logger = LogManager.get("quest_editor/gui/QuestEditorView");

const DEFAULT_LAYOUT_CONFIG = {
    settings: {
        showPopoutIcon: false,
        showMaximiseIcon: true,
        showCloseIcon: true,
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

export class QuestEditorView extends ResizableWidget {
    readonly element = el.div({ class: "quest_editor_QuestEditorView" });

    /**
     * Maps views to names and creation functions.
     */
    private readonly view_map: Map<
        new (...args: never) => ResizableWidget,
        { name: string; create(): ResizableWidget }
    >;

    private readonly layout_element = create_element("div", { class: "quest_editor_gl_container" });
    private readonly layout: Promise<GoldenLayout>;
    private loaded_layout: GoldenLayout | undefined;

    private readonly sub_views = new Map<string, ResizableWidget>();

    constructor(
        private readonly gui_store: GuiStore,
        quest_editor_store: QuestEditorStore,
        private readonly quest_editor_ui_persister: QuestEditorUiPersister,
        private readonly tool_bar: QuestEditorToolBar,
        create_quest_info_view: () => QuestInfoView,
        create_npc_counts_view: () => NpcCountsView,
        create_editor_renderer_view: () => QuestEditorRendererView,
        create_asm_editor_view: () => AsmEditorView,
        create_entity_info_view: () => EntityInfoView,
        create_npc_list_view: () => NpcListView,
        create_object_list_view: () => ObjectListView,
        create_events_view: () => EventsView,
        create_quest_runner_renderer_view: () => QuestRunnerRendererView,
        create_registers_view: () => RegistersView,
    ) {
        super();

        // Don't change the values of this map, as they are persisted in the user's browser.
        this.view_map = new Map<
            new (...args: never) => ResizableWidget,
            { name: string; create(): ResizableWidget }
        >([
            [
                QuestInfoView,
                {
                    name: "quest_info",
                    create: create_quest_info_view,
                },
            ],
            [NpcCountsView, { name: "npc_counts", create: create_npc_counts_view }],
            [
                QuestEditorRendererView,
                {
                    name: "quest_renderer",
                    create: create_editor_renderer_view,
                },
            ],
            [
                AsmEditorView,
                {
                    name: "asm_editor",
                    create: create_asm_editor_view,
                },
            ],
            [EntityInfoView, { name: "entity_info", create: create_entity_info_view }],
            [
                NpcListView,
                {
                    name: "npc_list_view",
                    create: create_npc_list_view,
                },
            ],
            [
                ObjectListView,
                {
                    name: "object_list_view",
                    create: create_object_list_view,
                },
            ],
        ]);

        if (gui_store.feature_active("events")) {
            this.view_map.set(EventsView, {
                name: "events_view",
                create: create_events_view,
            });
        }

        if (gui_store.feature_active("vm")) {
            this.view_map.set(QuestRunnerRendererView, {
                name: "quest_runner",
                create: create_quest_runner_renderer_view,
            });
            this.view_map.set(LogView, { name: "log_view", create: () => new LogView() });
            this.view_map.set(RegistersView, {
                name: "registers_view",
                create: create_registers_view,
            });
        }

        this.element.append(this.tool_bar.element, this.layout_element);

        this.layout = this.init_golden_layout();

        this.layout.then(layout => (this.loaded_layout = layout));

        this.disposables(
            gui_store.on_global_keydown(
                GuiTool.QuestEditor,
                "Ctrl-Alt-D",
                () => (quest_editor_store.debug.val = !quest_editor_store.debug.val),
            ),

            quest_editor_store.quest_runner.running.observe(async ({ value: running }) => {
                const layout = await this.layout;

                if (quest_editor_store.quest_runner.running.val === running) {
                    const runner_items = layout.root.getItemsById(
                        this.view_map.get(QuestRunnerRendererView)!.name,
                    );

                    if (running) {
                        if (runner_items.length === 0) {
                            const renderer_item = layout.root.getItemsById(
                                this.view_map.get(QuestEditorRendererView)!.name,
                            )[0];

                            renderer_item.parent.addChild({
                                id: this.view_map.get(QuestRunnerRendererView)!.name,
                                title: "Debug 3D View",
                                type: "component",
                                componentName: this.view_map.get(QuestRunnerRendererView)!.name,
                                isClosable: false,
                            });
                        }
                    } else {
                        for (const item of runner_items) {
                            item.remove();
                        }
                    }
                }
            }),
        );

        this.finalize_construction();
    }

    resize(width: number, height: number): this {
        super.resize(width, height);

        const layout_height = Math.max(0, height - this.tool_bar.height);
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
        const default_layout_content = this.get_default_layout_content();

        try {
            const content = await this.quest_editor_ui_persister.load_layout_config(
                default_layout_content,
            );

            if (content) {
                const gl = this.attempt_gl_init({
                    ...DEFAULT_LAYOUT_CONFIG,
                    content,
                });

                logger.info("Instantiated golden layout with persisted layout.");

                return gl;
            }
        } catch (e) {
            logger.warn("Couldn't instantiate golden layout with persisted layout.", e);
        }

        logger.info("Instantiating golden layout with default layout.");

        return this.attempt_gl_init({
            ...DEFAULT_LAYOUT_CONFIG,
            content: default_layout_content,
        });
    }

    private attempt_gl_init(config: GoldenLayout.Config): GoldenLayout {
        const layout = new GoldenLayout(config, this.layout_element);
        const sub_views = this.sub_views;

        try {
            for (const { name, create } of this.view_map.values()) {
                // registerComponent expects a regular function and not an arrow function. This
                // function will be called with new.
                layout.registerComponent(name, function(container: Container) {
                    let view: ResizableWidget;

                    try {
                        view = create();
                    } catch (e) {
                        logger.error(`Couldn't instantiate "${name}".`, e);

                        view = new ErrorView("Something went wrong while creating this window.");
                    }

                    container.on("close", () => view.dispose());
                    container.on("resize", () =>
                        // Subtract 4 from height to work around bug in Golden Layout related to
                        // headerHeight.
                        view.resize(container.width, container.height - 4),
                    );

                    view.resize(container.width, container.height);

                    sub_views.set(name, view);
                    container.getElement().append(view.element);
                });
            }

            layout.on("stateChanged", () => {
                this.quest_editor_ui_persister.persist_layout_config(layout.toConfig().content);
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

    private get_default_layout_content(): ItemConfigType[] {
        return [
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
                                        componentName: this.view_map.get(QuestInfoView)!.name,
                                        isClosable: false,
                                    },
                                    {
                                        title: "NPC Counts",
                                        type: "component",
                                        componentName: this.view_map.get(NpcCountsView)!.name,
                                        isClosable: false,
                                    },
                                ],
                            },
                            {
                                title: "Entity",
                                type: "component",
                                componentName: this.view_map.get(EntityInfoView)!.name,
                                isClosable: false,
                            },
                        ],
                    },
                    {
                        type: "stack",
                        width: 9,
                        content: [
                            {
                                id: this.view_map.get(QuestEditorRendererView)!.name,
                                title: "3D View",
                                type: "component",
                                componentName: this.view_map.get(QuestEditorRendererView)!.name,
                                isClosable: false,
                            },
                            {
                                title: "Script",
                                type: "component",
                                componentName: this.view_map.get(AsmEditorView)!.name,
                                isClosable: false,
                            },
                            ...(this.gui_store.feature_active("vm")
                                ? [
                                      {
                                          title: "Debug Log",
                                          type: "component",
                                          componentName: this.view_map.get(LogView)!.name,
                                          isClosable: false,
                                      },
                                      {
                                          title: "Registers",
                                          type: "component",
                                          componentName: this.view_map.get(RegistersView)!.name,
                                          isClosable: false,
                                      },
                                  ]
                                : []),
                        ],
                    },
                    {
                        type: "stack",
                        width: 2,
                        content: [
                            {
                                title: "NPCs",
                                type: "component",
                                componentName: this.view_map.get(NpcListView)!.name,
                                isClosable: false,
                            },
                            {
                                title: "Objects",
                                type: "component",
                                componentName: this.view_map.get(ObjectListView)!.name,
                                isClosable: false,
                            },
                            ...(this.gui_store.feature_active("events")
                                ? [
                                      {
                                          title: "Events",
                                          type: "component",
                                          componentName: this.view_map.get(EventsView)!.name,
                                          isClosable: false,
                                      },
                                  ]
                                : []),
                        ],
                    },
                ],
            },
        ];
    }
}
