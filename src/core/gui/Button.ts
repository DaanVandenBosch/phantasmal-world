import { el } from "./dom";
import "./Button.css";
import { Observable } from "../observable/Observable";
import { emitter } from "../observable";
import { Control } from "./Control";
import { Emitter } from "../observable/Emitter";
import { WidgetOptions } from "./Widget";

type ButtonOptions = WidgetOptions & {
    text?: string;
};

export class Button extends Control<HTMLButtonElement> {
    readonly click: Observable<MouseEvent>;

    private readonly _click: Emitter<MouseEvent> = emitter<MouseEvent>();

    constructor(text: string, options?: ButtonOptions) {
        super(
            el.button({ class: "core_Button" }, el.span({ class: "core_Button_inner", text })),
            options,
        );

        this.click = this._click;

        this.element.onclick = (e: MouseEvent) => this._click.emit({ value: e });
    }

    protected set_enabled(enabled: boolean): void {
        super.set_enabled(enabled);
        this.element.disabled = !enabled;
    }
}
