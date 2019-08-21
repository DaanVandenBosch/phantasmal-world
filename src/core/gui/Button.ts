import { create_el } from "./dom";
import { View } from "./View";
import "./Button.css";
import { Observable } from "../observable/Observable";
import { emitter } from "../observable";

export class Button extends View {
    readonly element: HTMLButtonElement = create_el("button", "core_Button");

    private readonly _click = emitter<MouseEvent>();
    readonly click: Observable<MouseEvent> = this._click;

    constructor(text: string) {
        super();

        const inner_element = create_el("span", "core_Button_inner");
        inner_element.textContent = text;

        this.element.append(inner_element);

        this.element.onclick = (e: MouseEvent) => this._click.emit(e, undefined);
    }
}
