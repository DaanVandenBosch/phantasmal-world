import { ViewOptions, Widget } from "./Widget";
import { create_element } from "./dom";
import { WritableProperty } from "../observable/WritableProperty";
import "./Label.css";
import { Property } from "../observable/Property";
import { WidgetProperty } from "../observable/WidgetProperty";

export class Label extends Widget {
    readonly element = create_element<HTMLLabelElement>("label", { class: "core_Label" });

    set for(id: string) {
        this.element.htmlFor = id;
    }

    readonly text: WritableProperty<string>;

    private readonly _text = new WidgetProperty<string>(this, "", this.set_text);

    constructor(text: string | Property<string>, options?: ViewOptions) {
        super(options);

        this.text = this._text;

        if (typeof text === "string") {
            this.set_text(text);
        } else {
            this.disposable(this._text.bind_to(text));
        }
    }

    protected set_text(text: string): void {
        this.element.textContent = text;
    }
}
