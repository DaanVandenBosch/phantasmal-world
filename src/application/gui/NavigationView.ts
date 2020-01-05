import { a, div, icon, Icon, span } from "../../core/gui/dom";
import "./NavigationView.css";
import { GuiStore, GuiTool } from "../../core/stores/GuiStore";
import { NavigationButton } from "./NavigationButton";
import { Select } from "../../core/gui/Select";
import { View } from "../../core/gui/View";

const TOOLS: [GuiTool, string][] = [
    [GuiTool.Viewer, "Viewer"],
    [GuiTool.QuestEditor, "Quest Editor"],
    [GuiTool.HuntOptimizer, "Hunt Optimizer"],
];

export class NavigationView extends View {
    private readonly buttons = new Map<GuiTool, NavigationButton>(
        TOOLS.map(([value, text]) => [value, this.add(new NavigationButton(value, text))]),
    );
    private readonly server_select = this.add(
        new Select({
            label: "Server:",
            items: ["Ephinea"],
            to_label: server => server,
            enabled: false,
            selected: "Ephinea",
            tooltip: "Only Ephinea is supported at the moment",
        }),
    );

    readonly element = div(
        { className: "application_NavigationView" },

        ...[...this.buttons.values()].map(button => button.element),

        div({ className: "application_NavigationView_spacer" }),

        span(
            { className: "application_NavigationView_server" },
            this.server_select.label!.element,
            this.server_select.element,
        ),

        a(
            {
                className: "application_NavigationView_github",
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

        this.disposables(
            gui_store.tool.observe(({ value }) => this.mark_tool_button(value), { call_now: true }),
        );

        this.finalize_construction();
    }

    private mousedown = (e: MouseEvent): void => {
        if (e.target instanceof HTMLLabelElement && e.target.control instanceof HTMLInputElement) {
            this.gui_store.set_tool((GuiTool as any)[e.target.control.value]);
        }
    };

    private mark_tool_button = (tool: GuiTool): void => {
        const button = this.buttons.get(tool);
        if (button) button.checked = true;
    };
}
