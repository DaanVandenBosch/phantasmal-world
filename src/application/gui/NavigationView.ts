import { el } from "../../core/gui/dom";
import "./NavigationView.css";
import { gui_store, GuiTool } from "../../core/stores/GuiStore";
import { View } from "../../core/gui/View";

const TOOLS: [GuiTool, string][] = [
    [GuiTool.Viewer, "Viewer"],
    [GuiTool.QuestEditor, "Quest Editor"],
    [GuiTool.HuntOptimizer, "Hunt Optimizer"],
];

export class NavigationView extends View {
    readonly element = el("div", { class: "application_NavigationView" });

    readonly height = 30;

    private buttons = new Map<GuiTool, ToolButton>(
        TOOLS.map(([value, text]) => [value, this.disposable(new ToolButton(value, text))]),
    );

    constructor() {
        super();

        this.element.style.height = `${this.height}px`;
        this.element.onclick = this.click;

        for (const button of this.buttons.values()) {
            this.element.append(button.element);
        }

        this.tool_changed(gui_store.tool.val);
        this.disposable(gui_store.tool.observe(this.tool_changed));
    }

    private click(e: MouseEvent): void {
        if (e.target instanceof HTMLLabelElement && e.target.control instanceof HTMLInputElement) {
            gui_store.tool.val = (GuiTool as any)[e.target.control.value];
        }
    }

    private tool_changed = (tool: GuiTool) => {
        const button = this.buttons.get(tool);
        if (button) button.checked = true;
    };
}

class ToolButton extends View {
    element: HTMLElement = el("span");

    private input: HTMLInputElement = el("input");
    private label: HTMLLabelElement = el("label");

    constructor(tool: GuiTool, text: string) {
        super();

        const tool_str = GuiTool[tool];

        this.input.type = "radio";
        this.input.name = "application_ToolButton";
        this.input.value = tool_str;
        this.input.id = `application_ToolButton_${tool_str}`;

        this.label.append(text);
        this.label.htmlFor = `application_ToolButton_${tool_str}`;

        this.element.className = "application_ToolButton";
        this.element.append(this.input, this.label);
    }

    set checked(checked: boolean) {
        this.input.checked = checked;
    }
}
