import { LabelledControl } from "./LabelledControl";
import { el } from "./dom";
import { property } from "../observable";
import { WritableProperty } from "../observable/WritableProperty";
import "./TextArea.css";

export class TextArea extends LabelledControl {
    readonly element: HTMLElement = el.div({ class: "core_TextArea" });

    readonly preferred_label_position = "left";

    readonly value: WritableProperty<string>;

    private readonly text_element: HTMLTextAreaElement = el.textarea({
        class: "core_TextArea_inner",
    });

    constructor(
        value = "",
        options?: {
            label?: string;
            max_length?: number;
            font_family?: string;
            rows?: number;
            cols?: number;
        },
    ) {
        super(options && options.label);

        if (options) {
            if (options.max_length != undefined) this.text_element.maxLength = options.max_length;
            if (options.font_family != undefined)
                this.text_element.style.fontFamily = options.font_family;
            if (options.rows != undefined) this.text_element.rows = options.rows;
            if (options.cols != undefined) this.text_element.cols = options.cols;
        }

        this.value = property(value);

        this.text_element.onchange = () => (this.value.val = this.text_element.value);

        this.disposables(this.value.observe(({ value }) => (this.text_element.value = value)));

        this.element.append(this.text_element);
    }
}
