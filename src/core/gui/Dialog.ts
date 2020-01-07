import { ResizableWidget } from "./ResizableWidget";
import { Widget } from "./Widget";
import { div, h1, li, section, ul } from "./dom";
import { Result } from "../Result";
import { Button } from "./Button";
import "./Dialog.css";

const DIALOG_WIDTH = 500;
const DIALOG_MAX_HEIGHT = 500;

export class Dialog extends ResizableWidget {
    private x = 0;
    private y = 0;
    private prev_mouse_x = 0;
    private prev_mouse_y = 0;
    private readonly overlay_element: HTMLElement;

    readonly element: HTMLElement;
    readonly children: readonly Widget[] = [];
    readonly dismiss_button = this.disposable(new Button({ text: "Dismiss" }));

    constructor(title: string, description: string, content: Node | string) {
        super();

        let header_element: HTMLElement;

        this.element = section(
            { className: "core_Dialog", tabIndex: 0 },
            (header_element = h1(title)),
            div({ className: "core_Dialog_description" }, description),
            div({ className: "core_Dialog_body" }, content),
            div({ className: "core_Dialog_footer" }, this.dismiss_button.element),
        );

        this.element.style.width = `${DIALOG_WIDTH}px`;
        this.element.style.maxHeight = `${DIALOG_MAX_HEIGHT}px`;

        this.set_position(
            (window.innerWidth - DIALOG_WIDTH) / 2,
            (window.innerHeight - DIALOG_MAX_HEIGHT) / 2,
        );

        this.element.addEventListener("keydown", this.keydown);
        header_element.addEventListener("mousedown", this.mousedown);

        this.overlay_element = div({ className: "core_Dialog_modal_overlay", tabIndex: -1 });
        this.overlay_element.addEventListener("focus", () => this.element.focus());
        document.body.append(this.overlay_element);

        this.disposables(this.dismiss_button.onclick.observe(() => this.dispose()));

        this.finalize_construction();
    }

    dispose(): void {
        super.dispose();
        this.overlay_element.remove();
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
            this.dispose();
        }
    };
}

/**
 * Shows a dialog window if `result` failed or succeeded with problems.
 *
 * @param result
 * @param problems_message - Message to show if problems occurred when result is successful.
 * @param error_message - Message to show if result failed.
 */
export function show_result_dialog(
    result: Result<unknown>,
    problems_message: string,
    error_message: string,
): void {
    let dialog: Dialog | undefined;

    if (!result.success) {
        dialog = new Dialog("Error", error_message, create_result_body(result));
    } else if (result.problems.length) {
        dialog = new Dialog("Problems", problems_message, create_result_body(result));
    }

    if (dialog) {
        document.body.append(dialog.element);
        dialog.focus();
    }
}

function create_result_body(result: Result<unknown>): HTMLElement {
    const body = ul(...result.problems.map(problem => li(problem.ui_message)));
    body.style.cursor = "text";
    return body;
}
