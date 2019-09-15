import { el } from "../../core/gui/dom";
import { gui_store, GuiTool } from "../../core/stores/GuiStore";
import { LazyWidget } from "../../core/gui/LazyWidget";
import { ResizableWidget } from "../../core/gui/ResizableWidget";
import { ChangeEvent } from "../../core/observable/Observable";

const TOOLS: [GuiTool, () => Promise<ResizableWidget>][] = [
    [GuiTool.Viewer, async () => new (await import("../../viewer/gui/ViewerView")).ViewerView()],
    [
        GuiTool.QuestEditor,
        async () => new (await import("../../quest_editor/gui/QuestEditorView")).QuestEditorView(),
    ],
    [
        GuiTool.HuntOptimizer,
        async () =>
            new (await import("../../hunt_optimizer/gui/HuntOptimizerView")).HuntOptimizerView(),
    ],
];

export class MainContentView extends ResizableWidget {
    readonly element = el.div({ class: "application_MainContentView" });

    private tool_views = new Map(
        TOOLS.map(([tool, create_view]) => [tool, this.disposable(new LazyWidget(create_view))]),
    );

    constructor() {
        super();

        for (const tool_view of this.tool_views.values()) {
            this.element.append(tool_view.element);
        }

        const tool_view = this.tool_views.get(gui_store.tool.val);
        if (tool_view) tool_view.visible.val = true;

        this.disposable(gui_store.tool.observe(this.tool_changed));

        this.finalize_construction(MainContentView.prototype);
    }

    resize(width: number, height: number): this {
        super.resize(width, height);

        for (const tool_view of this.tool_views.values()) {
            tool_view.resize(width, height);
        }

        return this;
    }

    private tool_changed = ({ value: new_tool }: ChangeEvent<GuiTool>) => {
        for (const tool of this.tool_views.values()) {
            tool.visible.val = false;
        }

        const new_view = this.tool_views.get(new_tool);
        if (new_view) new_view.visible.val = true;
    };
}
