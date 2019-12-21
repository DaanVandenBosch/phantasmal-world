import { el } from "../../core/gui/dom";
import { GuiStore, GuiTool } from "../../core/stores/GuiStore";
import { LazyWidget } from "../../core/gui/LazyWidget";
import { ResizableWidget } from "../../core/gui/ResizableWidget";
import { ChangeEvent } from "../../core/observable/Observable";

export class MainContentView extends ResizableWidget {
    readonly element = el.div({ class: "application_MainContentView" });

    private tool_views: Map<GuiTool, LazyWidget>;

    constructor(gui_store: GuiStore, tool_views: [GuiTool, () => Promise<ResizableWidget>][]) {
        super();

        this.tool_views = new Map(
            tool_views.map(([tool, create_view]) => [
                tool,
                this.disposable(new LazyWidget(create_view)),
            ]),
        );

        for (const tool_view of this.tool_views.values()) {
            this.element.append(tool_view.element);
        }

        const tool_view = this.tool_views.get(gui_store.tool.val);
        if (tool_view) tool_view.visible.val = true;

        this.disposable(gui_store.tool.observe(this.tool_changed));

        this.finalize_construction();
    }

    resize(width: number, height: number): this {
        super.resize(width, height);

        for (const tool_view of this.tool_views.values()) {
            tool_view.resize(width, height);
        }

        return this;
    }

    private tool_changed = ({ value: new_tool }: ChangeEvent<GuiTool>): void => {
        for (const tool of this.tool_views.values()) {
            tool.visible.val = false;
        }

        const new_view = this.tool_views.get(new_tool);
        if (new_view) new_view.visible.val = true;
    };
}
