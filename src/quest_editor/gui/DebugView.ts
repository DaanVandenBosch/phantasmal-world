import { bind_children_to, div, Icon } from "../../core/gui/dom";
import { ToolBar } from "../../core/gui/ToolBar";
import "./DebugView.css";
import { Select } from "../../core/gui/Select";
import { LogEntry, time_to_string } from "../../core/Logger";
import { ResizableView } from "../../core/gui/ResizableView";
import { Severities, Severity } from "../../core/Severity";
import { Button } from "../../core/gui/Button";
import { DebugController } from "../controllers/DebugController";

const AUTOSCROLL_TRESHOLD = 5;

export class DebugView extends ResizableView {
    readonly element: HTMLElement;

    // container is needed to get a scrollbar in the right place
    private readonly list_container: HTMLElement;
    private readonly list_element: HTMLElement;

    private readonly settings_bar: ToolBar;

    private should_scroll_to_bottom = true;

    constructor(ctrl: DebugController) {
        super();

        const debug_button = new Button({
            text: "Debug",
            icon_left: Icon.Play,
            tooltip: "Debug the current quest in a virtual machine (F5)",
        });
        const resume_button = new Button({
            text: "Continue",
            icon_left: Icon.SquareArrowRight,
            tooltip: "Resume execution (F6)",
        });
        const step_over_button = new Button({
            text: "Step over",
            icon_left: Icon.LongArrowRight,
            tooltip: "Execute the next line and step over any function calls (F8)",
        });
        const step_in_button = new Button({
            text: "Step into",
            icon_left: Icon.LevelDown,
            tooltip: "Execute the next line and step inside any function calls (F7)",
        });
        const step_out_button = new Button({
            text: "Step out",
            icon_left: Icon.LevelUp,
            tooltip: "Execute until outside of current call frame (Shift-F8)",
        });
        const stop_button = new Button({
            text: "Stop",
            icon_left: Icon.Stop,
            tooltip: "Stop execution (Shift-F5)",
        });
        const severity_filter = new Select({
            class: "quest_editor_DebugView_severity",
            label: "Severity:",
            items: Severities,
            selected: ctrl.severity,
            to_label: severity => Severity[severity],
        });
        // TODO: ensure label is up-to-date.
        const thread_select = new Select({
            label: "Thread:",
            items: ctrl.thread_ids,
            to_label: id => {
                const status = ctrl.active_thread_id.val === id ? "Active" : "Yielded";
                return `Thread #${id} (${status})`;
            },
        });

        this.settings_bar = this.add(
            new ToolBar(
                { class: "quest_editor_DebugView_settings" },
                debug_button,
                resume_button,
                step_over_button,
                step_in_button,
                step_out_button,
                stop_button,
                severity_filter,
                thread_select,
            ),
        );

        this.list_element = div({ className: "quest_editor_DebugView_message_list" });

        this.list_container = div(
            { className: "quest_editor_DebugView_list_container" },
            this.list_element,
        );

        this.element = div(
            {
                className: "quest_editor_DebugView",
                tabIndex: -1,
            },
            this.settings_bar.element,
            this.list_container,
        );

        this.list_container.addEventListener("scroll", this.scrolled);

        this.disposables(
            bind_children_to(this.list_element, ctrl.log, this.create_message_element, {
                after: this.scroll_to_bottom,
            }),

            debug_button.onclick.observe(ctrl.debug),
            debug_button.enabled.bind_to(ctrl.can_debug),

            resume_button.onclick.observe(ctrl.resume),
            resume_button.enabled.bind_to(ctrl.can_step),

            step_over_button.onclick.observe(ctrl.step_over),
            step_over_button.enabled.bind_to(ctrl.can_step),

            step_in_button.onclick.observe(ctrl.step_in),
            step_in_button.enabled.bind_to(ctrl.can_step),

            step_out_button.onclick.observe(ctrl.step_out),
            step_out_button.enabled.bind_to(ctrl.can_step),

            stop_button.onclick.observe(ctrl.stop),
            stop_button.enabled.bind_to(ctrl.can_stop),

            severity_filter.selected.observe(
                ({ value }) => value != undefined && ctrl.set_severity(value),
            ),

            thread_select.selected.observe(({ value }) => ctrl.select_thread(value!)),
            thread_select.selected.bind_to(ctrl.debugging_thread_id),
            thread_select.enabled.bind_to(ctrl.can_select_thread),
        );

        this.finalize_construction();
    }

    private scrolled = (): void => {
        this.should_scroll_to_bottom =
            this.list_container.scrollTop >=
            this.list_container.scrollHeight -
                this.list_container.offsetHeight -
                AUTOSCROLL_TRESHOLD;
    };

    private scroll_to_bottom = (): void => {
        if (this.should_scroll_to_bottom) {
            this.list_container.scrollTo({
                top: this.list_container.scrollHeight,
                left: 0,
                behavior: "auto",
            });
        }
    };

    private create_message_element = ({ time, severity, message }: LogEntry): HTMLElement => {
        return div(
            {
                className: [
                    "quest_editor_DebugView_message",
                    "quest_editor_DebugView_" + Severity[severity] + "_message",
                ].join(" "),
            },
            div({ className: "quest_editor_DebugView_message_timestamp" }, time_to_string(time)),
            div(
                { className: "quest_editor_DebugView_message_severity" },
                `[${Severity[severity]}]`,
            ),
            div({ className: "quest_editor_DebugView_message_contents" }, message),
        );
    };
}
