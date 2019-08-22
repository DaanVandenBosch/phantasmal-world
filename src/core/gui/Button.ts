import { el } from "./dom";
import "./Button.css";
import { Observable } from "../observable/Observable";
import { emitter } from "../observable";
import { Control } from "./Control";

export class Button extends Control {
    readonly element: HTMLButtonElement = el("button", { class: "core_Button" });

    private readonly _click = emitter<MouseEvent>();
    readonly click: Observable<MouseEvent> = this._click;

    constructor(text: string) {
        super();

        this.element.append(el("span", { class: "core_Button_inner", text }));

        this.enabled.observe(enabled => (this.element.disabled = !enabled));

        this.element.onclick = (e: MouseEvent) => this._click.emit(e);
    }
}
