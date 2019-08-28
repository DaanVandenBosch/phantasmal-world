import { el } from "../../core/gui/dom";
import "./NavigationView.css";
import { gui_store, GuiTool } from "../../core/stores/GuiStore";
import { Widget } from "../../core/gui/Widget";
import { NavigationButton } from "./NavigationButton";

const TOOLS: [GuiTool, string][] = [
    [GuiTool.Viewer, "Viewer"],
    [GuiTool.QuestEditor, "Quest Editor"],
    [GuiTool.HuntOptimizer, "Hunt Optimizer"],
];

export class NavigationView extends Widget {
    readonly height = 30;

    private buttons = new Map<GuiTool, NavigationButton>(
        TOOLS.map(([value, text]) => [value, this.disposable(new NavigationButton(value, text))]),
    );

    constructor() {
        super(el.div({ class: "application_NavigationView" }));

        this.element.style.height = `${this.height}px`;
        this.element.onmousedown = this.mousedown;

        for (const button of this.buttons.values()) {
            this.element.append(button.element);
        }

        this.mark_tool_button(gui_store.tool.val);
        this.disposable(gui_store.tool.observe(({ value }) => this.mark_tool_button(value)));
    }

    private mousedown(e: MouseEvent): void {
        if (e.target instanceof HTMLLabelElement && e.target.control instanceof HTMLInputElement) {
            gui_store.tool.val = (GuiTool as any)[e.target.control.value];
        }
    }

    private mark_tool_button = (tool: GuiTool) => {
        const button = this.buttons.get(tool);
        if (button) button.checked = true;
    };
}
