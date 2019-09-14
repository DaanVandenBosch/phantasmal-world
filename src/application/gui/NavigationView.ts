import { el, icon, Icon } from "../../core/gui/dom";
import "./NavigationView.css";
import { gui_store, GuiTool } from "../../core/stores/GuiStore";
import { Widget } from "../../core/gui/Widget";
import { NavigationButton } from "./NavigationButton";
import { Select } from "../../core/gui/Select";
import { property } from "../../core/observable";

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

        this.element.append(el.div({ class: "application_NavigationView_spacer" }));

        const server_select = this.disposable(
            new Select(property(["Ephinea"]), server => server, {
                label: "Server:",
                enabled: false,
                selected: "Ephinea",
                tooltip: "Only Ephinea is supported at the moment",
            }),
        );

        this.element.append(
            el.span(
                { class: "application_NavigationView_server" },
                server_select.label!.element,
                server_select.element,
            ),
            el.a(
                {
                    class: "application_NavigationView_github",
                    href: "https://github.com/DaanVandenBosch/phantasmal-world",
                    title: "GitHub",
                },
                icon(Icon.GitHub),
            ),
        );

        this.mark_tool_button(gui_store.tool.val);
        this.disposable(gui_store.tool.observe(({ value }) => this.mark_tool_button(value)));

        this.finalize_construction(NavigationView.prototype);
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
