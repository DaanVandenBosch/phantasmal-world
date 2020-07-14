import { GuiStore, GuiTool } from "../../core/stores/GuiStore";
import { LazyWidget } from "../../core/gui/LazyWidget";
import { div } from "../../core/gui/dom";
import { ResizableView } from "../../core/gui/ResizableView";
import { Widget } from "../../core/gui/Widget";
import { Resizable } from "../../core/gui/Resizable";

export class MainContentView extends ResizableView {
    private tool_views: Map<GuiTool, LazyWidget>;
    private current_tool_view?: LazyWidget;

    readonly element = div({ className: "application_MainContentView" });

    constructor(gui_store: GuiStore, tool_views: [GuiTool, () => Promise<Widget & Resizable>][]) {
        super();

        this.tool_views = new Map(
            tool_views.map(([tool, create_view]) => [tool, this.add(new LazyWidget(create_view))]),
        );

        for (const tool_view of this.tool_views.values()) {
            this.element.append(tool_view.element);
        }

        this.disposables(
            gui_store.tool.observe(({ value }) => this.set_current_tool(value), { call_now: true }),
        );

        this.finalize_construction(MainContentView);
    }

    resize(width: number, height: number): this {
        super.resize(width, height);

        for (const tool_view of this.tool_views.values()) {
            tool_view.resize(width, height);
        }

        return this;
    }

    private set_current_tool(tool: GuiTool): void {
        if (this.current_tool_view) {
            this.current_tool_view.visible.val = false;
            this.current_tool_view.deactivate();
        }

        this.current_tool_view = this.tool_views.get(tool);

        if (this.current_tool_view) {
            this.current_tool_view.visible.val = true;
            this.current_tool_view.activate();
        }
    }
}
