import { View } from "./View";
import { create_element } from "./dom";
import { WritableProperty } from "../observable/WritableProperty";
import "./Label.css";
import { property } from "../observable";
import { Property } from "../observable/Property";

export class Label extends View {
    readonly element = create_element<HTMLLabelElement>("label", { class: "core_Label" });

    set for(id: string) {
        this.element.htmlFor = id;
    }

    readonly enabled: WritableProperty<boolean> = property(true);

    constructor(text: string | Property<string>, options: { enabled?: boolean } = {}) {
        super();

        if (typeof text === "string") {
            this.element.append(text);
        } else {
            this.element.append(text.val);
            this.disposable(text.observe(({ value }) => (this.element.textContent = value)));
        }

        this.disposables(
            this.enabled.observe(({ value }) => {
                if (value) {
                    this.element.classList.remove("disabled");
                } else {
                    this.element.classList.add("disabled");
                }
            }),
        );

        if (options.enabled != undefined) this.enabled.val = options.enabled;
    }
}
