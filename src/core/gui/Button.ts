import { button, Icon, icon, span } from "./dom";
import "./Button.css";
import { Observable } from "../observable/Observable";
import { emitter } from "../observable";
import { Control, ControlOptions } from "./Control";
import { Property } from "../observable/property/Property";
import { WritableProperty } from "../observable/property/WritableProperty";
import { WidgetProperty } from "../observable/property/WidgetProperty";

export type ButtonOptions = ControlOptions & {
    readonly text?: string | Property<string>;
    readonly icon_left?: Icon;
    readonly icon_right?: Icon;
};

export class Button extends Control {
    private readonly _onmouseup = emitter<MouseEvent>();
    private readonly _onclick = emitter<MouseEvent>();
    private readonly _onkeydown = emitter<KeyboardEvent>();
    private readonly _text: WidgetProperty<string>;
    private readonly center_element: HTMLSpanElement;

    readonly element = button({ className: "core_Button" });
    readonly onmouseup: Observable<MouseEvent> = this._onmouseup;
    readonly onclick: Observable<MouseEvent> = this._onclick;
    readonly onkeydown: Observable<KeyboardEvent> = this._onkeydown;
    readonly text: WritableProperty<string>;

    constructor(options?: ButtonOptions) {
        super(options);

        const inner_element = span({ className: "core_Button_inner" });

        if (options?.icon_left != undefined) {
            inner_element.append(span({ className: "core_Button_left" }, icon(options.icon_left)));
        }

        this.center_element = span({ className: "core_Button_center" });
        inner_element.append(this.center_element);

        if (options?.icon_right != undefined) {
            inner_element.append(
                span({ className: "core_Button_right" }, icon(options.icon_right)),
            );
        }

        this.element.onmouseup = (e: MouseEvent) => this._onmouseup.emit({ value: e });
        this.element.onclick = (e: MouseEvent) => this._onclick.emit({ value: e });
        this.element.onkeydown = (e: KeyboardEvent) => this._onkeydown.emit({ value: e });

        this._text = new WidgetProperty<string>(this, "", this.set_text);
        this.text = this._text;

        if (typeof options?.text === "string") {
            this.text.val = options.text;
        } else if (options?.text) {
            this.text.bind_to(options.text);
        } else {
            this.text.val = "";
        }

        this.element.append(inner_element);

        this.finalize_construction();
    }

    protected set_enabled(enabled: boolean): void {
        super.set_enabled(enabled);
        this.element.disabled = !enabled;
    }

    protected set_text(text: string): void {
        this.center_element.textContent = text;
        this.center_element.hidden = text === "";
    }
}
