import { create_el } from "../../core/gui/dom";
import { View } from "../../core/gui/View";
import { gui_store, GuiTool } from "../../core/stores/GuiStore";
import { LazyView } from "../../core/gui/LazyView";
import { Resizable } from "../../core/gui/Resizable";
import { ResizableView } from "../../core/gui/ResizableView";

const TOOLS: [GuiTool, () => Promise<View & Resizable>][] = [
    [GuiTool.Viewer, async () => new (await import("../../viewer/gui/ViewerView")).ViewerView()],
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

        this.tool_changed(gui_store.tool, gui_store.tool);
        this.disposable(gui_store.tool_prop.observe(this.tool_changed));
    }

    resize(width: number, height: number): this {
        super.resize(width, height);

        for (const tool_view of this.tool_views.values()) {
            tool_view.resize(width, height);
        }

        return this;
    }

    private tool_changed = (new_tool: GuiTool, old_tool: GuiTool) => {
        const old_view = this.tool_views.get(old_tool);
        if (old_view) old_view.visible = false;

        const new_view = this.tool_views.get(new_tool);
        if (new_view) new_view.visible = true;
    };
}
