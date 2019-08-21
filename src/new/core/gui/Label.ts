import { View } from "./View";
import { create_el } from "./dom";
import { WritableProperty } from "../observable/WritableProperty";
import "./Label.css";
import { property } from "../observable";
import { Property } from "../observable/Property";

export class Label extends View {
    readonly element = create_el<HTMLLabelElement>("label", "core_Label");

    set for(id: string) {
        this.element.htmlFor = id;
    }

    readonly enabled: WritableProperty<boolean> = property(true);

    constructor(text: string | Property<string>) {
        super();

        if (typeof text === "string") {
            this.element.append(text);
        } else {
            this.element.append(text.get());
            this.disposable(text.observe(text => (this.element.textContent = text)));
        }

        this.disposables(
            this.enabled.observe(enabled => {
                if (enabled) {
                    this.element.classList.remove("disabled");
                } else {
                    this.element.classList.add("disabled");
                }
            }),
        );
    }
}
