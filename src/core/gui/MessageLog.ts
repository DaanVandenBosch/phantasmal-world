import { el, bind_children_to } from "./dom";
import { ResizableWidget } from "./ResizableWidget";
import { ListProperty } from "../observable/property/list/ListProperty";
import { Property } from "../observable/property/Property";
import { DropDown } from "./DropDown";
import { ToolBar } from "./ToolBar";
import { list_property } from "../observable";
import "./MessageLog.css";

export interface LogLevel {
    readonly name: string;
    readonly value: number;
}

export interface LogGroup {
    readonly name: string;
    readonly key: number;
}

export interface LogMessage {
    readonly formatted_timestamp: string;
    readonly message_contents: string;
    readonly log_level: LogLevel;
    readonly log_group: LogGroup;
}

export interface MessageLogStore {
    log_messages: ListProperty<LogMessage>;
    log_level: Property<LogLevel>;
    log_group: Property<LogGroup>;
    log_levels: readonly LogLevel[];
    log_groups: ListProperty<LogGroup>;

    set_log_level(level: LogLevel): void;
    set_log_group(group: LogGroup): void;
    show_all_log_groups(): void;
}

export class MessageLog extends ResizableWidget {
    readonly element = el.div({ class: "MessageLog", tab_index: -1 });
    protected base_classname: string;

    // container is needed to get a scrollbar in the right place
    protected list_container: HTMLElement;
    protected list_element: HTMLElement;

    protected level_filter: DropDown<LogLevel>;
    protected group_filter: DropDown<LogGroup>;
    protected settings_bar: ToolBar;

    protected was_scrolled_to_bottom = true;

    constructor(protected store: MessageLogStore, classname: string) {
        super();

        this.base_classname = this.element.className;
        this.element.classList.add(classname);

        this.list_container = el.div({ class: this.base_classname + "_list_container" });
        this.list_element = el.div({ class: this.base_classname + "_message_list" });

        this.level_filter = new DropDown("Level", this.store.log_levels, l => l.name, {
            class: this.base_classname + "_level_filter",
        });

        const show_all: LogGroup = {
            name: "Show All",
            key: -1,
        };

        // prepend fake "show all" option at the start
        const group_list = list_property(undefined, show_all, ...this.store.log_groups.val);

        this.group_filter = new DropDown("Group", group_list, g => g.name, {
            class: this.base_classname + "_group_filter",
        });

        this.settings_bar = new ToolBar({
            class: this.base_classname + "_settings",
            children: [this.level_filter, this.group_filter],
        });

        this.disposables(
            // before update, save scroll state
            this.store.log_messages.observe_list(() => {
                this.was_scrolled_to_bottom = this.is_scrolled_to_bottom();
            }),

            // do update
            bind_children_to(
                this.list_element,
                this.store.log_messages,
                this.create_message_element,
            ),

            // after update, scroll if was scrolled
            this.store.log_messages.observe_list(() => {
                if (this.was_scrolled_to_bottom) {
                    this.scroll_to_bottom();
                }
            }),

            this.level_filter.chosen.observe(({ value }) => this.store.set_log_level(value)),

            this.group_filter.chosen.observe(({ value }) => {
                // special case for "show all" option
                if (value.key === show_all.key) {
                    this.store.show_all_log_groups();
                } else {
                    this.store.set_log_group(value);
                }
            }),

            this.store.log_groups.observe(() => {
                // prepend fake "show all" option at the start
                group_list.val = [show_all].concat(this.store.log_groups.val);
            }),
        );

        this.list_container.appendChild(this.list_element);
        this.element.appendChild(this.settings_bar.element);
        this.element.appendChild(this.list_container);

        this.finalize_construction(this.constructor.prototype);
    }

    protected get_formatted_timestamp(date = new Date()): string {
        return "[" + date.toISOString() + "]";
    }

    /**
     * How far away from the bottom the scrolling is allowed to
     * be for autoscroll to still happen. Returns pixels.
     */
    protected get_autoscroll_treshold(): number {
        const some_msg = this.list_element.firstElementChild;

        if (!some_msg) {
            return 0;
        }

        // half of the height of a message
        return some_msg.clientHeight / 2;
    }

    protected is_scrolled_to_bottom(): boolean {
        return (
            this.list_container.scrollTop >=
            this.list_container.scrollHeight -
                this.list_container.offsetHeight -
                this.get_autoscroll_treshold()
        );
    }

    protected scroll_to_bottom(): void {
        this.list_container.scrollTo({
            top: this.list_container.scrollHeight,
            left: 0,
            behavior: "auto",
        });
    }

    protected add_message(msg: HTMLElement): void {
        const autoscroll = this.is_scrolled_to_bottom();

        this.list_element.appendChild(msg);

        if (autoscroll) {
            this.scroll_to_bottom();
        }
    }

    protected create_message_element = (msg: LogMessage): HTMLElement => {
        return el.div(
            {
                class: [
                    this.base_classname + "_message",
                    this.base_classname + "_" + msg.log_level.name + "_message",
                ].join(" "),
            },
            el.div({
                class: this.base_classname + "_message_timestamp",
                text: msg.formatted_timestamp,
            }),
            el.div({
                class: this.base_classname + "_message_group",
                text: "[" + msg.log_group.name + "]",
            }),
            el.div({
                class: this.base_classname + "_message_level",
                text: "[" + msg.log_level.name + "]",
            }),
            el.div({
                class: this.base_classname + "_message_contents",
                text: msg.message_contents,
            }),
        );
    };
}
