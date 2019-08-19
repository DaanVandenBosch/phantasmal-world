import { create_el } from "./dom";
import { View } from "./View";
import "./FileInput.css";
import "./Button.css";

function dummy_function(): void {}

export class FileInput extends View {
    private input: HTMLInputElement = create_el("input", "core_FileInput_input");

    element: HTMLLabelElement = create_el("label", "core_Button");

    constructor(text: string, accept: string = "") {
        super();

        this.input.type = "file";
        this.input.accept = accept;
        this.input.onchange = () => {
            if (this.input.files && this.input.files.length) {
                this.on_files_chosen([...this.input.files!]);
            }
        };

        this.element.textContent = text;
        this.element.append(this.input);
    }

    on_files_chosen: (files: File[]) => void = dummy_function;
}
