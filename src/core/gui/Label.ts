import { WidgetOptions, Widget } from "./Widget";
import { create_element } from "./dom";
import { WritableProperty } from "../observable/property/WritableProperty";
import "./Label.css";
import { Property } from "../observable/property/Property";
import { WidgetProperty } from "../observable/property/WidgetProperty";

export class Label extends Widget {
    readonly element = create_element<HTMLLabelElement>("label", { class: "core_Label" });

    set for(id: string) {
        this.element.htmlFor = id;
    }

    readonly text: WritableProperty<string>;

    private readonly _text = new WidgetProperty<string>(this, "", this.set_text);

    constructor(text: string | Property<string>, options?: WidgetOptions) {
        super(options);

        this.text = this._text;

        if (typeof text === "string") {
            this.set_text(text);
        } else {
            this.disposable(this._text.bind_to(text));
        }

        this.finalize_construction();
    }

    protected set_text(text: string): void {
        this.element.textContent = text;
    }
}
