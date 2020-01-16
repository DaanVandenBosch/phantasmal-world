import { Dialog } from "./Dialog";
import { Button } from "./Button";
import { Result } from "../Result";
import { is_property, Property } from "../observable/property/Property";
import { div, li, ul } from "./dom";
import { property } from "../observable";
import { WidgetOptions } from "./Widget";

/**
 * Does not inherit {@link Dialog}'s options. The parent class' options are determined by this
 * class.
 */
export type ResultDialogOptions = WidgetOptions & {
    readonly result?: Result<unknown> | Property<Result<unknown> | undefined>;
    /**
     * Message to show if problems occurred when result is successful.
     */
    readonly problems_message: string | Property<string>;
    /**
     * Message to show if result failed.
     */
    readonly error_message: string | Property<string>;
};

/**
 * Shows the details of a result if the result failed or succeeded with problems. Shows a "Dismiss"
 * button in the footer which triggers emission of a dismiss event.
 */
export class ResultDialog extends Dialog {
    private readonly problems_message: Property<string>;
    private readonly error_message: Property<string>;

    constructor(options: ResultDialogOptions) {
        const dismiss_button = new Button({ text: "Dismiss" });

        super({ footer: [dismiss_button.element], ...options });

        const result: Property<Result<unknown> | undefined> = is_property(options.result)
            ? options.result
            : property(options.result);

        this.problems_message = is_property(options.problems_message)
            ? options.problems_message
            : property(options.problems_message);

        this.error_message = is_property(options.error_message)
            ? options.error_message
            : property(options.error_message);

        this.disposables(
            dismiss_button,
            dismiss_button.onclick.observe(evt => this._ondismiss.emit(evt)),

            result.observe(({ value }) => this.result_changed(value), { call_now: true }),
        );

        this.finalize_construction();
    }

    private result_changed(result?: Result<unknown>): void {
        if (result) {
            this.content.val = create_result_body(result);

            if (!result.success) {
                this.title.val = "Error";
                this.description.val = this.error_message.val;
            } else if (result.problems.length) {
                this.title.val = "Problems";
                this.description.val = this.problems_message.val;
            }
        } else {
            this.content.val = "";
        }
    }
}

function create_result_body(result: Result<unknown>): HTMLElement {
    const body = div();
    body.style.overflow = "auto";
    body.style.userSelect = "text";
    body.style.height = "100%";
    body.style.maxHeight = "400px"; // Workaround for chrome bug.

    const list_element = ul(...result.problems.map(problem => li(problem.ui_message)));
    list_element.style.cursor = "text";
    body.append(list_element);

    return body;
}
