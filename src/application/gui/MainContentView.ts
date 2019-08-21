import { create_el } from "../../core/gui/dom";
import { gui_store, GuiTool } from "../../core/stores/GuiStore";
import { LazyView } from "../../core/gui/LazyView";
import { ResizableView } from "../../core/gui/ResizableView";

const TOOLS: [GuiTool, () => Promise<ResizableView>][] = [
    [GuiTool.Viewer, async () => new (await import("../../viewer/gui/ViewerView")).ViewerView()],
    [
        GuiTool.QuestEditor,
        async () => new (await import("../../quest_editor/gui/QuestEditorView")).QuestEditorView(),
    ],
];

export class MainContentView extends ResizableView {
    element = create_el("div", "application_MainContentView");

    private tool_views = new Map(
        TOOLS.map(([tool, create_view]) => [tool, this.disposable(new LazyView(create_view))]),
    );

    constructor() {
        super();

        for (const tool_view of this.tool_views.values()) {
            this.element.append(tool_view.element);
        }

        const tool_view = this.tool_views.get(gui_store.tool.get());
        if (tool_view) tool_view.visible = true;

        this.disposable(gui_store.tool.observe(this.tool_changed));
    }

    resize(width: number, height: number): this {
        super.resize(width, height);

        for (const tool_view of this.tool_views.values()) {
            tool_view.resize(width, height);
        }

        return this;
    }

    private tool_changed = (new_tool: GuiTool, { old_value }: { old_value: GuiTool }) => {
        const old_view = this.tool_views.get(old_value);
        if (old_view) old_view.visible = false;

        const new_view = this.tool_views.get(new_tool);
        if (new_view) new_view.visible = true;
    };
}
