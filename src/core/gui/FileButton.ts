import { create_el } from "./dom";
import { View } from "./View";
import "./FileButton.css";
import "./Button.css";
import { property } from "../observable";
import { Property } from "../observable/Property";

export class FileButton extends View {
    readonly element: HTMLLabelElement = create_el("label", "core_FileButton core_Button");

    private readonly _files = property<File[]>([]);
    readonly files: Property<File[]> = this._files;

    private input: HTMLInputElement = create_el("input", "core_FileButton_input");

    constructor(text: string, accept: string = "") {
        super();

        this.input.type = "file";
        this.input.accept = accept;
        this.input.onchange = () => {
            if (this.input.files && this.input.files.length) {
                this._files.set([...this.input.files!]);
            } else {
                this._files.set([]);
            }
        };

        this.element.textContent = text;
        this.element.append(this.input);
    }
}
