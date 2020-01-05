import { RendererWidget } from "../../core/gui/RendererWidget";
import { QuestRenderer } from "../rendering/QuestRenderer";
import { QuestEditorStore } from "../stores/QuestEditorStore";
import { div } from "../../core/gui/dom";
import { ResizableView } from "../../core/gui/ResizableView";

export abstract class QuestRendererView extends ResizableView {
    private readonly renderer_view: RendererWidget;

    protected readonly renderer: QuestRenderer;

    readonly element: HTMLElement;

    protected constructor(
        quest_editor_store: QuestEditorStore,
        className: string,
        renderer: QuestRenderer,
    ) {
        super();

        this.element = div({ className: className, tabIndex: -1 });
        this.renderer = renderer;
        this.renderer_view = this.add(new RendererWidget(this.renderer));
        this.element.append(this.renderer_view.element);

        this.disposables(
            quest_editor_store.debug.observe(({ value }) => (this.renderer.debug = value)),
        );

        this.finalize_construction();
    }

    activate(): void {
        this.renderer_view.start_rendering();
        super.activate();
    }

    deactivate(): void {
        super.deactivate();
        this.renderer_view.stop_rendering();
    }

    resize(width: number, height: number): this {
        super.resize(width, height);

        this.renderer_view.resize(width, height);

        return this;
    }
}
