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

        this.element.textContent = text;

        this.element.onclick = (e: MouseEvent) => this._click.emit(e, undefined);
    }
}
