import { ResizableView } from "../../core/gui/ResizableView";
import { create_el } from "../../core/gui/dom";
import { ToolBarView } from "./ToolBarView";
import GoldenLayout, { ContentItem } from "golden-layout";
import { quest_editor_ui_persister } from "../persistence/QuestEditorUiPersister";
import { AssemblyEditorComponent } from "../../old/quest_editor/ui/AssemblyEditorComponent";
import { quest_editor_store } from "../../old/quest_editor/stores/QuestEditorStore";
import Logger = require("js-logger");

const logger = Logger.get("quest_editor/gui/QuestEditorView");

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

export class QuestEditorView extends ResizableView {
    readonly element = create_el("div");

    private readonly tool_bar_view = this.disposable(new ToolBarView());

    private layout_element = create_el("div");
    // private layout: GoldenLayout;

    constructor() {
        super();

        // const content = await quest_editor_ui_persister.load_layout_config(
        //     [...CMP_TO_NAME.values()],
        //     DEFAULT_LAYOUT_CONTENT,
        // );
        //
        // const config: GoldenLayout.Config = {
        //     ...DEFAULT_LAYOUT_CONFIG,
        //     content,
        // };
        //
        // try {
        //     this.layout = new GoldenLayout(config, this.layout_element);
        // } catch (e) {
        //     logger.warn("Couldn't initialize golden layout with persisted layout.", e);
        //
        //     this.layout = new GoldenLayout(
        //         {
        //             ...DEFAULT_LAYOUT_CONFIG,
        //             content: DEFAULT_LAYOUT_CONTENT,
        //         },
        //         this.layout_element,
        //     );
        // }
        //
        // for (const [component, name] of CMP_TO_NAME) {
        //     this.layout.registerComponent(name, component);
        // }
        //
        // this.layout.on("stateChanged", () => {
        //     if (this.layout) {
        //         quest_editor_ui_persister.persist_layout_config(this.layout.toConfig().content);
        //     }
        // });
        //
        // this.layout.on("stackCreated", (stack: ContentItem) => {
        //     stack.on("activeContentItemChanged", (item: ContentItem) => {
        //         if ("component" in item.config) {
        //             if (item.config.component === CMP_TO_NAME.get(AssemblyEditorComponent)) {
        //                 quest_editor_store.script_undo.make_current();
        //             } else {
        //                 quest_editor_store.undo.make_current();
        //             }
        //         }
        //     });
        // });
        //
        // this.layout.init();

        this.element.append(this.tool_bar_view.element, this.layout_element);
    }
}
