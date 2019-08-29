import { LabelledControl, LabelledControlOptions } from "./LabelledControl";
import { el } from "./dom";
import { WritableProperty } from "../observable/property/WritableProperty";
import "./TextArea.css";
import { WidgetProperty } from "../observable/property/WidgetProperty";

export type TextAreaOptions = LabelledControlOptions & {
    max_length?: number;
    font_family?: string;
    rows?: number;
    cols?: number;
};

export class TextArea extends LabelledControl {
    readonly preferred_label_position = "left";

    readonly value: WritableProperty<string>;

    private readonly text_element: HTMLTextAreaElement = el.textarea({
        class: "core_TextArea_inner",
    });

    private readonly _value = new WidgetProperty<string>(this, "", this.set_value);

    constructor(value = "", options?: TextAreaOptions) {
        super(el.div({ class: "core_TextArea" }), options);

        if (options) {
            if (options.max_length != undefined) this.text_element.maxLength = options.max_length;
            if (options.font_family != undefined)
                this.text_element.style.fontFamily = options.font_family;
            if (options.rows != undefined) this.text_element.rows = options.rows;
            if (options.cols != undefined) this.text_element.cols = options.cols;
        }

        this.value = this._value;
        this.set_value(value);

        this.text_element.onchange = () =>
            this._value.set_val(this.text_element.value, { silent: false });

        this.element.append(this.text_element);
    }

    protected set_value(value: string): void {
        this.text_element.value = value;
    }
}
