import { ResizableWidget } from "../../core/gui/ResizableWidget";
import { el } from "../../core/gui/dom";
import { RendererWidget } from "../../core/gui/RendererWidget";
import { QuestRenderer } from "../rendering/QuestRenderer";
import { gui_store, GuiTool } from "../../core/stores/GuiStore";
import { QuestRunnerModelManager } from "../rendering/QuestRunnerModelManager";

export class QuestRunnerRendererView extends ResizableWidget {
    readonly element = el.div({ class: "quest_editor_QuestRunnerRendererView", tab_index: -1 });

    private renderer_view = this.disposable(
        new RendererWidget(new QuestRenderer(QuestRunnerModelManager)),
    );

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

        this.finalize_construction();
    }

    resize(width: number, height: number): this {
        super.resize(width, height);

        this.renderer_view.resize(width, height);

        return this;
    }
}
