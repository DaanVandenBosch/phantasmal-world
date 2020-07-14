import { GuiTool } from "../../core/stores/GuiStore";
import "./NavigationButton.css";
import { input, label, span } from "../../core/gui/dom";
import { Control } from "../../core/gui/Control";

export class NavigationButton extends Control {
    readonly element = span({ className: "application_NavigationButton" });

    private input: HTMLInputElement = input();
    private label: HTMLLabelElement = label();

    constructor(tool: GuiTool, text: string) {
        super();

        const tool_str = GuiTool[tool];

        this.input.type = "radio";
        this.input.name = "application_NavigationButton";
        this.input.value = tool_str;
        this.input.id = `application_NavigationButton_${tool_str}`;

        this.label.append(text);
        this.label.htmlFor = `application_NavigationButton_${tool_str}`;

        this.element.append(this.input, this.label);

        this.finalize_construction(NavigationButton);
    }

    set checked(checked: boolean) {
        this.input.checked = checked;
    }
}
