import { create_element } from "./dom";
import "./Button.css";
import { Observable } from "../observable/Observable";
import { emitter } from "../observable";
import { Control } from "./Control";
import { Emitter } from "../observable/Emitter";
import { ViewOptions } from "./Widget";

export class Button extends Control {
    readonly element: HTMLButtonElement = create_element("button", { class: "core_Button" });

    readonly click: Observable<MouseEvent>;

    private readonly _click: Emitter<MouseEvent> = emitter<MouseEvent>();

    constructor(text: string, options?: ViewOptions) {
        super(options);

        this.click = this._click;

        this.element.append(create_element("span", { class: "core_Button_inner", text }));

        this.disposables(this.enabled.observe(({ value }) => (this.element.disabled = !value)));

        this.element.onclick = (e: MouseEvent) => this._click.emit({ value: e });
    }

    protected set_enabled(enabled: boolean): void {
        super.set_enabled(enabled);
        this.element.disabled = !enabled;
    }
}
