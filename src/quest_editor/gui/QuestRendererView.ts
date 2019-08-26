import { ResizableView } from "../../core/gui/ResizableView";
import { el } from "../../core/gui/dom";
import { RendererView } from "../../core/gui/RendererView";
import { QuestRenderer } from "../rendering/QuestRenderer";
import { gui_store, GuiTool } from "../../core/stores/GuiStore";

export class QuestRendererView extends ResizableView {
    readonly element = el.div({ class: "quest_editor_QuestRendererView" });

    private renderer_view = this.disposable(new RendererView(new QuestRenderer()));

    constructor() {
        super();

        this.element.append(this.renderer_view.element);

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
