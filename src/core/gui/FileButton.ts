import { create_element } from "./dom";
import "./FileButton.css";
import "./Button.css";
import { property } from "../observable";
import { Property } from "../observable/Property";
import { Control } from "./Control";

export class FileButton extends Control {
    readonly element: HTMLLabelElement = create_element("label", {
        class: "core_FileButton core_Button",
    });

    private readonly _files = property<File[]>([]);
    readonly files: Property<File[]> = this._files;

    private input: HTMLInputElement = create_element("input", {
        class: "core_FileButton_input core_Button_inner",
    });

    constructor(text: string, accept: string = "") {
        super();

        this.input.type = "file";
        this.input.accept = accept;
        this.input.onchange = () => {
            if (this.input.files && this.input.files.length) {
                this._files.val = [...this.input.files!];
            } else {
                this._files.val = [];
            }
        };

        this.element.append(
            create_element("span", {
                class: "core_FileButton_inner core_Button_inner",
                text,
            }),
            this.input,
        );

        this.disposables(
            this.enabled.observe(({ value }) => {
                this.input.disabled = !value;

                if (value) {
                    this.element.classList.remove("disabled");
                } else {
                    this.element.classList.add("disabled");
                }
            }),
        );
    }
}
