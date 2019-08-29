import { el, Icon, icon } from "./dom";
import "./Button.css";
import { Observable } from "../observable/Observable";
import { emitter } from "../observable";
import { Control } from "./Control";
import { Emitter } from "../observable/Emitter";
import { WidgetOptions } from "./Widget";
import { Property } from "../observable/property/Property";
import { WritableProperty } from "../observable/property/WritableProperty";
import { WidgetProperty } from "../observable/property/WidgetProperty";

export type ButtonOptions = WidgetOptions & {
    icon_left?: Icon;
    icon_right?: Icon;
};

export class Button extends Control<HTMLButtonElement> {
    readonly mousedown: Observable<MouseEvent>;
    readonly mouseup: Observable<MouseEvent>;
    readonly click: Observable<MouseEvent>;
    readonly text: WritableProperty<string>;

    private readonly _mousedown: Emitter<MouseEvent>;
    private readonly _mouseup: Emitter<MouseEvent>;
    private readonly _click: Emitter<MouseEvent>;
    private readonly _text: WidgetProperty<string>;
    private readonly center_element: HTMLSpanElement;

    constructor(text: string | Property<string>, options?: ButtonOptions) {
        const inner_element = el.span({ class: "core_Button_inner" });

        super(el.button({ class: "core_Button" }, inner_element), options);

        if (options && options.icon_left != undefined) {
            inner_element.append(el.span({ class: "core_Button_left" }, icon(options.icon_left)));
        }

        this.center_element = el.span({ class: "core_Button_center" });
        inner_element.append(this.center_element);

        if (options && options.icon_right != undefined) {
            inner_element.append(el.span({ class: "core_Button_right" }, icon(options.icon_right)));
        }

        this._mousedown = emitter<MouseEvent>();
        this.mousedown = this._mousedown;
        this.element.onmousedown = (e: MouseEvent) => this._mousedown.emit({ value: e });

        this._mouseup = emitter<MouseEvent>();
        this.mouseup = this._mouseup;
        this.element.onmouseup = (e: MouseEvent) => this._mouseup.emit({ value: e });

        this._click = emitter<MouseEvent>();
        this.click = this._click;
        this.element.onclick = (e: MouseEvent) => this._click.emit({ value: e });

        this._text = new WidgetProperty<string>(this, "", this.set_text);
        this.text = this._text;

        if (typeof text === "string") {
            this.text.val = text;
        } else if (text) {
            this.text.bind_to(text);
        }
    }

    protected set_enabled(enabled: boolean): void {
        super.set_enabled(enabled);
        this.element.disabled = !enabled;
    }

    protected set_text(text: string): void {
        this.center_element.textContent = text;
    }
}
