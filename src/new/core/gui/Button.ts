import { create_el } from "./dom";
import { View } from "./View";
import "./Button.css";

function dummy_function(): void {}

export class Button extends View {
    element: HTMLButtonElement = create_el("button", "core_Button");

    constructor(text: string) {
        super();

        this.element.textContent = text;

        this.element.onclick = () => this.on_click();
    }

    on_click: () => void = dummy_function;
}
