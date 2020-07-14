import { Widget, WidgetOptions } from "./Widget";
import { WritableProperty } from "../observable/property/WritableProperty";
import "./Label.css";
import { Property } from "../observable/property/Property";
import { WidgetProperty } from "../observable/property/WidgetProperty";
import { label } from "./dom";

export class Label extends Widget {
    private readonly _text = new WidgetProperty<string>(this, "", this.set_text);

    readonly element = label({ className: "core_Label" });
    readonly children: readonly Widget[] = [];

    set for(id: string) {
        this.element.htmlFor = id;
    }

    readonly text: WritableProperty<string> = this._text;

    constructor(text: string | Property<string>, options?: WidgetOptions) {
        super(options);

        if (typeof text === "string") {
            this.set_text(text);
        } else {
            this.disposable(this._text.bind_to(text));
        }

        this.finalize_construction(Label);
    }

    protected set_text(text: string): void {
        this.element.textContent = text;
    }
}
