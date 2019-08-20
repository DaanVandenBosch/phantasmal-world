import { create_el } from "./dom";
import { View } from "./View";
import "./FileInput.css";
import "./Button.css";
import { Property } from "../observable/Property";

export class FileInput extends View {
    private input: HTMLInputElement = create_el("input", "core_FileInput_input");

    element: HTMLLabelElement = create_el("label", "core_FileInput core_Button");

    constructor(text: string, accept: string = "") {
        super();

        this.input.type = "file";
        this.input.accept = accept;
        this.input.onchange = () => {
            if (this.input.files && this.input.files.length) {
                this.files.set([...this.input.files!]);
            } else {
                this.files.set([]);
            }
        };

        this.element.textContent = text;
        this.element.append(this.input);
    }

    readonly files = new Property<File[]>([]);
}
