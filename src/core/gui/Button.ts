import { create_element } from "./dom";
import "./Button.css";
import { Observable } from "../observable/Observable";
import { emitter } from "../observable";
import { Control } from "./Control";

export class Button extends Control {
    readonly element: HTMLButtonElement = create_element("button", { class: "core_Button" });

    private readonly _click = emitter<MouseEvent>();
    readonly click: Observable<MouseEvent> = this._click;

    constructor(text: string) {
        super();

        this.element.append(create_element("span", { class: "core_Button_inner", text }));

        this.disposables(this.enabled.observe(({ value }) => (this.element.disabled = !value)));

        this.element.onclick = (e: MouseEvent) => this._click.emit({ value: e });
    }
}
