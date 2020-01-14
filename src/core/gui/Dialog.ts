import { ResizableWidget } from "./ResizableWidget";
import { Widget, WidgetOptions } from "./Widget";
import { Child, div, h1, section } from "./dom";
import "./Dialog.css";
import { is_property, Property } from "../observable/property/Property";
import { WidgetProperty } from "../observable/property/WidgetProperty";
import { WritableProperty } from "../observable/property/WritableProperty";
import { Emitter } from "../observable/Emitter";
import { Observable } from "../observable/Observable";
import { emitter } from "../observable";

const DIALOG_WIDTH = 500;
const DIALOG_MAX_HEIGHT = 500;

export type DialogOptions = WidgetOptions & {
    readonly title?: string | Property<string>;
    readonly description?: string | Property<string>;
    readonly content?: Child | Property<Child>;
    readonly footer?: readonly Child[];
};

/**
 * A popup window with a title, description, body and dismiss button.
 */
export class Dialog extends ResizableWidget {
    private x = 0;
    private y = 0;
    private prev_mouse_x = 0;
    private prev_mouse_y = 0;

    private _title = new WidgetProperty<string>(this, "", this.set_title);
    private _description = new WidgetProperty<string>(this, "", this.set_description);
    private _content = new WidgetProperty<Child>(this, "", this.set_content);

    private readonly overlay_element: HTMLElement;
    private readonly header_element: HTMLElement;
    private readonly description_element: HTMLElement;
    private readonly content_element: HTMLElement;

    protected readonly _ondismiss: Emitter<Event> = emitter();

    readonly element: HTMLElement;
    readonly children: readonly Widget[] = [];

    readonly title: WritableProperty<string> = this._title;
    readonly description: WritableProperty<string> = this._description;
    readonly content: WritableProperty<Child> = this._content;

    /**
     * Emits an event when the user presses the escape key.
     */
    readonly ondismiss: Observable<Event> = this._ondismiss;

    constructor(options?: DialogOptions) {
        super(options);

        this.element = section(
            { className: "core_Dialog", tabIndex: 0 },
            (this.header_element = h1()),
            (this.description_element = div({ className: "core_Dialog_description" })),
            (this.content_element = div({ className: "core_Dialog_body" })),
            div({ className: "core_Dialog_footer" }, ...(options?.footer ?? [])),
        );

        this.element.style.width = `${DIALOG_WIDTH}px`;
        this.element.style.maxHeight = `${DIALOG_MAX_HEIGHT}px`;

        this.element.addEventListener("keydown", evt => this.keydown(evt));

        if (options) {
            if (typeof options.title === "string") {
                this.title.val = options.title;
            } else if (options.title) {
                this.title.bind_to(options.title);
            }

            if (typeof options.description === "string") {
                this.description.val = options.description;
            } else if (options.description) {
                this.description.bind_to(options.description);
            }

            if (is_property(options.content)) {
                this.content.bind_to(options.content);
            } else if (options.content != undefined) {
                this.content.val = options.content;
            }
        }

        this.set_position(
            (window.innerWidth - DIALOG_WIDTH) / 2,
            (window.innerHeight - DIALOG_MAX_HEIGHT) / 2,
        );

        this.header_element.addEventListener("mousedown", this.mousedown);

        this.overlay_element = div({ className: "core_Dialog_modal_overlay", tabIndex: -1 });
        this.overlay_element.addEventListener("focus", () => this.focus());

        this.finalize_construction();
    }

    dispose(): void {
        super.dispose();
        this.overlay_element.remove();
    }

    focus(): void {
        (this.first_focusable_child(this.element) || this.element).focus();
    }

    private first_focusable_child(element: HTMLElement): HTMLElement | undefined {
        for (const child of element.children) {
            if (child instanceof HTMLElement) {
                if (child.tabIndex >= 0) {
                    return child;
                } else {
                    const element = this.first_focusable_child(child);

                    if (element) {
                        return element;
                    }
                }
            }
        }

        return undefined;
    }

    set_position(x: number, y: number): void {
        this.x = x;
        this.y = y;
        this.element.style.transform = `translate(${Math.floor(x)}px, ${Math.floor(y)}px)`;
    }

    protected set_visible(visible: boolean): void {
        if (visible) {
            document.body.append(this.overlay_element);
            document.body.append(this.element);
            this.focus();
        } else {
            this.overlay_element.remove();
            this.element.remove();
        }
    }

    private set_title(title: string): void {
        this.header_element.textContent = title;
    }

    private set_description(description: string): void {
        if (description === "") {
            this.description_element.hidden = true;
            this.description_element.textContent = "";
        } else {
            this.description_element.hidden = false;
            this.description_element.textContent = description;
        }
    }

    private set_content(content: Child): void {
        this.content_element.textContent = "";
        this.content_element.append(content);
    }

    private mousedown = (evt: MouseEvent): void => {
        this.prev_mouse_x = evt.clientX;
        this.prev_mouse_y = evt.clientY;
        window.addEventListener("mousemove", this.window_mousemove);
        window.addEventListener("mouseup", this.window_mouseup);
    };

    private window_mousemove = (evt: MouseEvent): void => {
        evt.preventDefault();
        this.set_position(
            this.x + evt.clientX - this.prev_mouse_x,
            this.y + evt.clientY - this.prev_mouse_y,
        );
        this.prev_mouse_x = evt.clientX;
        this.prev_mouse_y = evt.clientY;
    };

    private window_mouseup = (evt: MouseEvent): void => {
        evt.preventDefault();
        window.removeEventListener("mousemove", this.window_mousemove);
        window.removeEventListener("mouseup", this.window_mouseup);
    };

    private keydown(evt: KeyboardEvent): void {
        if (evt.key === "Escape") {
            this._ondismiss.emit({ value: evt });
        }
    }
}
