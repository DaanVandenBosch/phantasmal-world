import { create_el } from "./dom";
import { View } from "./View";
import "./Button.css";

export class Button extends View {
    element: HTMLButtonElement = create_el("button", "core_Button");

    constructor(text: string) {
        super();

        this.element.textContent = text;
    }
}
