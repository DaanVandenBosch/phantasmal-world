import { View } from "./View";
import { create_el } from "./dom";
import "./CheckBox.css";
import { Property } from "../observable/Property";

export class CheckBox extends View {
    private input: HTMLInputElement = create_el("input");

    element: HTMLLabelElement = create_el("label", "core_CheckBox");

    constructor(text: string) {
        super();

        this.input.type = "checkbox";
        this.input.onchange = () => this.checked.set(this.input.checked);

        this.element.append(this.input, text);
    }

    checked = new Property<boolean>(false);
}
