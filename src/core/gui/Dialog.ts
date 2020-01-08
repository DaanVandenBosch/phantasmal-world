import { ResizableWidget } from "./ResizableWidget";
import { Widget } from "./Widget";
import { div, h1, li, section, ul } from "./dom";
import { Result } from "../Result";
import { Button } from "./Button";
import "./Dialog.css";

const DIALOG_WIDTH = 500;
const DIALOG_MAX_HEIGHT = 500;

/**
 * A popup window with a title, description, body and dismiss button.
 */
export class Dialog extends ResizableWidget {
    private x = 0;
    private y = 0;
    private prev_mouse_x = 0;
    private prev_mouse_y = 0;
    private readonly overlay_element: HTMLElement;
    private readonly header_element = h1();
    private readonly description_element = div({ className: "core_Dialog_description" });
    private readonly content_element = div({ className: "core_Dialog_body" });
    private readonly dismiss_button = this.disposable(new Button({ text: "Dismiss" }));
    private readonly footer_element = div(
        { className: "core_Dialog_footer" },
        this.dismiss_button.element,
    );

    readonly element: HTMLElement = section(
        { className: "core_Dialog", tabIndex: 0 },
        this.header_element,
        this.description_element,
        this.content_element,
        this.footer_element,
    );
    readonly children: readonly Widget[] = [];

    set title(title: string) {
        this.header_element.textContent = title;
    }

    set description(description: string) {
        this.description_element.textContent = description;
    }

    set content(content: Node | string) {
        this.content_element.textContent = "";
        this.content_element.append(content);
    }

    constructor(title: string = "", description: string = "", content: Node | string = "") {
        super();

        this.title = title;
        this.description = description;
        this.content = content;

        this.element.style.width = `${DIALOG_WIDTH}px`;
        this.element.style.maxHeight = `${DIALOG_MAX_HEIGHT}px`;

        this.set_position(
            (window.innerWidth - DIALOG_WIDTH) / 2,
            (window.innerHeight - DIALOG_MAX_HEIGHT) / 2,
        );

        this.element.addEventListener("keydown", this.keydown);
        this.header_element.addEventListener("mousedown", this.mousedown);

        this.overlay_element = div({ className: "core_Dialog_modal_overlay", tabIndex: -1 });
        this.overlay_element.addEventListener("focus", () => this.element.focus());

        this.disposables(this.dismiss_button.onclick.observe(() => this.hide()));

        this.finalize_construction();
    }

    dispose(): void {
        super.dispose();
        this.overlay_element.remove();
    }

    show(): void {
        document.body.append(this.overlay_element);
        document.body.append(this.element);
        this.focus();
    }

    hide(): void {
        this.overlay_element.remove();
        this.element.remove();
    }

    set_position(x: number, y: number): void {
        this.x = x;
        this.y = y;
        this.element.style.transform = `translate(${Math.floor(x)}px, ${Math.floor(y)}px)`;
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

    private keydown = (evt: KeyboardEvent): void => {
        if (evt.key === "Escape") {
            this.hide();
        }
    };
}

/**
 * Shows the details of a result in a dialog window if the result failed or succeeded with problems.
 *
 * @param dialog
 * @param result
 * @param problems_message - Message to show if problems occurred when result is successful.
 * @param error_message - Message to show if result failed.
 */
export function show_result_in_dialog(
    dialog: Dialog,
    result: Result<unknown>,
    problems_message: string,
    error_message: string,
): void {
    dialog.content = create_result_body(result);

    if (!result.success) {
        dialog.title = "Error";
        dialog.description = error_message;
        dialog.show();
    } else if (result.problems.length) {
        dialog.title = "Problems";
        dialog.description = problems_message;
        dialog.show();
    }
}

function create_result_body(result: Result<unknown>): HTMLElement {
    const body = ul(...result.problems.map(problem => li(problem.ui_message)));
    body.style.cursor = "text";
    return body;
}
