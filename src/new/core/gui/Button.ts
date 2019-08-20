import { create_el } from "./dom";
import { View } from "./View";
import "./Button.css";
import { Observable } from "../observable/Observable";

export class Button extends View {
    element: HTMLButtonElement = create_el("button", "core_Button");

    constructor(text: string) {
        super();

        this.element.textContent = text;

        this.element.onclick = (e: MouseEvent) => this.click.fire(e, undefined);
    }

    click = new Observable<MouseEvent>();
}
