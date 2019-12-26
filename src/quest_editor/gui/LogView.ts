import { bind_children_to, div } from "../../core/gui/dom";
import { ResizableWidget } from "../../core/gui/ResizableWidget";
import { ToolBar } from "../../core/gui/ToolBar";
import "./LogView.css";
import { log_store } from "../stores/LogStore";
import { Select } from "../../core/gui/Select";
import { LogEntry, LogLevel, LogLevels, time_to_string } from "../../core/Logger";

const AUTOSCROLL_TRESHOLD = 5;

export class LogView extends ResizableWidget {
    readonly element = div({ className: "quest_editor_LogView", tabIndex: -1 });

    // container is needed to get a scrollbar in the right place
    private readonly list_container: HTMLElement;
    private readonly list_element: HTMLElement;

    private readonly level_filter: Select<LogLevel>;
    private readonly settings_bar: ToolBar;

    private should_scroll_to_bottom = true;

    constructor() {
        super();

        this.list_container = div({ className: "quest_editor_LogView_list_container" });
        this.list_element = div({ className: "quest_editor_LogView_message_list" });

        this.level_filter = this.disposable(
            new Select({
                class: "quest_editor_LogView_level_filter",
                label: "Level:",
                items: LogLevels,
                to_label: level => LogLevel[level],
            }),
        );

        this.settings_bar = this.disposable(
            new ToolBar({
                class: "quest_editor_LogView_settings",
                children: [this.level_filter],
            }),
        );

        this.list_container.addEventListener("scroll", this.scrolled);

        this.disposables(
            bind_children_to(this.list_element, log_store.log, this.create_message_element, {
                after: this.scroll_to_bottom,
            }),

            this.level_filter.selected.observe(
                ({ value }) => value != undefined && log_store.set_level(value),
            ),

            log_store.level.observe(
                ({ value }) => {
                    this.level_filter.selected.val = value;
                },
                { call_now: true },
            ),
        );

        this.list_container.appendChild(this.list_element);
        this.element.appendChild(this.settings_bar.element);
        this.element.appendChild(this.list_container);

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

    private create_message_element = ({ time, level, message }: LogEntry): HTMLElement => {
        return div(
            {
                className: [
                    "quest_editor_LogView_message",
                    "quest_editor_LogView_" + LogLevel[level] + "_message",
                ].join(" "),
            },
            div({ className: "quest_editor_LogView_message_timestamp" }, time_to_string(time)),
            div({ className: "quest_editor_LogView_message_level" }, "[" + LogLevel[level] + "]"),
            div({ className: "quest_editor_LogView_message_contents" }, message),
        );
    };
}
