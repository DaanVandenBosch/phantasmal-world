import { ResizableWidget } from "../../core/gui/ResizableWidget";
import { el } from "../../core/gui/dom";
import { RendererWidget } from "../../core/gui/RendererWidget";
import { QuestRenderer } from "../rendering/QuestRenderer";
import { gui_store, GuiTool } from "../../core/stores/GuiStore";
import { quest_editor_store } from "../stores/QuestEditorStore";

export class QuestRendererView extends ResizableWidget {
    private renderer_view = this.disposable(new RendererWidget(new QuestRenderer()));

    constructor() {
        super(el.div({ class: "quest_editor_QuestRendererView", tab_index: -1 }));

        this.element.append(this.renderer_view.element);

        this.element.addEventListener("focus", () => quest_editor_store.undo.make_current(), true);

        this.renderer_view.start_rendering();

        this.disposables(
            gui_store.tool.observe(({ value: tool }) => {
                if (tool === GuiTool.QuestEditor) {
                    this.renderer_view.start_rendering();
                } else {
                    this.renderer_view.stop_rendering();
                }
            }),
        );
    }

    resize(width: number, height: number): this {
        super.resize(width, height);

        this.renderer_view.resize(width, height);

        return this;
    }
}
