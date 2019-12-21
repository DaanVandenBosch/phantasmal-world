import { el, icon, Icon } from "../../core/gui/dom";
import "./NavigationView.css";
import { GuiStore, GuiTool } from "../../core/stores/GuiStore";
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
    private readonly buttons = new Map<GuiTool, NavigationButton>(
        TOOLS.map(([value, text]) => [value, this.disposable(new NavigationButton(value, text))]),
    );
    private readonly server_select = this.disposable(
        new Select(property(["Ephinea"]), server => server, {
            label: "Server:",
            enabled: false,
            selected: "Ephinea",
            tooltip: "Only Ephinea is supported at the moment",
        }),
    );

    readonly element = el.div(
        { class: "application_NavigationView" },

        ...[...this.buttons.values()].map(button => button.element),

        el.div({ class: "application_NavigationView_spacer" }),

        el.span(
            { class: "application_NavigationView_server" },
            this.server_select.label!.element,
            this.server_select.element,
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

    readonly height = 30;

    constructor(private readonly gui_store: GuiStore) {
        super();

        this.element.style.height = `${this.height}px`;
        this.element.onmousedown = this.mousedown;

        this.mark_tool_button(gui_store.tool.val);
        this.disposable(gui_store.tool.observe(({ value }) => this.mark_tool_button(value)));

        this.finalize_construction();
    }

    private mousedown = (e: MouseEvent): void => {
        if (e.target instanceof HTMLLabelElement && e.target.control instanceof HTMLInputElement) {
            this.gui_store.tool.val = (GuiTool as any)[e.target.control.value];
        }
    };

    private mark_tool_button = (tool: GuiTool): void => {
        const button = this.buttons.get(tool);
        if (button) button.checked = true;
    };
}
