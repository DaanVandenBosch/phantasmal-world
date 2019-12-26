import { ResizableWidget } from "../../core/gui/ResizableWidget";
import { RendererWidget } from "../../core/gui/RendererWidget";
import { QuestRenderer } from "../rendering/QuestRenderer";
import { GuiStore, GuiTool } from "../../core/stores/GuiStore";
import { QuestEditorStore } from "../stores/QuestEditorStore";
import { div } from "../../core/gui/dom";

export abstract class QuestRendererView extends ResizableWidget {
    private readonly renderer_view: RendererWidget;

    protected readonly renderer: QuestRenderer;

    readonly element: HTMLElement;

    protected constructor(
        gui_store: GuiStore,
        quest_editor_store: QuestEditorStore,
        className: string,
        renderer: QuestRenderer,
    ) {
        super();

        this.element = div({ className: className, tabIndex: -1 });
        this.renderer = renderer;
        this.renderer_view = this.disposable(new RendererWidget(this.renderer));
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

            quest_editor_store.debug.observe(({ value }) => (this.renderer.debug = value)),
        );

        this.finalize_construction();
    }

    resize(width: number, height: number): this {
        super.resize(width, height);

        this.renderer_view.resize(width, height);

        return this;
    }
}
