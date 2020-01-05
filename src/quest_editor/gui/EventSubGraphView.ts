import { bind_children_to, div } from "../../core/gui/dom";
import { QuestEventDagModel, QuestEventDagModelChangeType } from "../model/QuestEventDagModel";
import { QuestEventModel } from "../model/QuestEventModel";
import { EventView } from "./EventView";
import { EventsController } from "../controllers/EventsController";
import { Disposable } from "../../core/observable/Disposable";
import "./EventSubGraphView.css";
import { Disposer } from "../../core/observable/Disposer";
import {
    ListChangeEvent,
    ListChangeType,
    ListProperty,
} from "../../core/observable/property/list/ListProperty";
import { WritableProperty } from "../../core/observable/property/WritableProperty";
import { LogManager } from "../../core/Logger";
import { View } from "../../core/gui/View";

const logger = LogManager.get("quest_editor/gui/EventSubGraphView");

const EDGE_HORIZONTAL_SPACING = 8;
const EDGE_VERTICAL_SPACING = 20;

export class EventSubGraphView extends View {
    /**
     * Maps event IDs to GUI data.
     */
    private readonly event_gui_data: Map<
        QuestEventModel,
        { event_view: EventView; position: number }
    > = new Map();

    private readonly event_container_element = div({
        className: "quest_editor_EventSubGraphView_event_container",
    });

    private readonly edge_container_element = div({
        className: "quest_editor_EventSubGraphView_edge_container",
    });

    readonly element = div(
        { className: "quest_editor_EventSubGraphView" },
        this.edge_container_element,
        this.event_container_element,
    );

    constructor(
        private readonly ctrl: EventsController,
        private readonly dag: QuestEventDagModel,
        private readonly sub_graph: ListProperty<QuestEventModel>,
        private readonly max_edge_depth: WritableProperty<number>,
    ) {
        super();

        this.disposables(
            max_edge_depth.observe(
                ({ value }) => {
                    this.element.style.marginLeft = `${EDGE_HORIZONTAL_SPACING * value}px`;
                },
                { call_now: true },
            ),

            bind_children_to(this.event_container_element, sub_graph, this.create_event_element, {
                after: this.after_events_changed,
            }),

            dag.observe(({ value: change }) => {
                if (
                    change.type === QuestEventDagModelChangeType.EdgeAdded ||
                    change.type === QuestEventDagModelChangeType.EdgeRemoved
                ) {
                    this.update_edges();
                }
            }),
        );

        this.finalize_construction();
    }

    protected set_enabled(enabled: boolean): void {
        super.set_enabled(enabled);

        for (const { event_view } of this.event_gui_data.values()) {
            event_view.enabled.val = enabled;
        }
    }

    /**
     * This method does measurements of the event elements. So it should be called after the event
     * elements have been added to the DOM and have been laid out by the browser.
     */
    update_edges = (): void => {
        this.edge_container_element.innerHTML = "";

        if (this.sub_graph.length.val === 0) return;

        // Each edge has a different depth (higher depth means further distance from event nodes).
        // Keep track of the used depths here, to ensure edges never overlap.
        const used_depths: boolean[][] = Array(this.sub_graph.length.val - 1);

        for (let i = 0; i < used_depths.length; i++) {
            used_depths[i] = [];
        }

        for (const event of this.sub_graph) {
            const data = this.event_gui_data.get(event);

            if (!data) {
                logger.warn(`No GUI data for event ${event.id}.`);
                continue;
            }

            const { event_view, position } = data;
            const event_element = event_view.element;

            const y_offset =
                event_element.offsetTop + event_element.offsetHeight - EDGE_VERTICAL_SPACING;

            for (const child of this.dag.get_children(event)) {
                const child_data = this.event_gui_data.get(child);

                if (!child_data) {
                    logger.warn(`No GUI data for child event ${child.id}.`);
                    continue;
                }

                const { event_view: child_event_view, position: child_position } = child_data;
                const child_element = child_event_view.element;
                const child_y_offset = child_element.offsetTop + EDGE_VERTICAL_SPACING;

                const top = Math.min(y_offset, child_y_offset);
                const height = Math.max(y_offset, child_y_offset) - top;

                let depth = 1;
                const downwards = child_position > position;
                const low_pos = Math.min(position, child_position);
                const high_pos = Math.max(position, child_position);

                outer: while (true) {
                    for (
                        let i = downwards ? low_pos : Math.max(0, low_pos - 1);
                        i < high_pos;
                        i++
                    ) {
                        if (used_depths[i][depth]) {
                            depth++;
                            continue outer;
                        }
                    }

                    break;
                }

                for (let i = low_pos; i < high_pos; i++) {
                    used_depths[i][depth] = true;
                }

                this.max_edge_depth.val = Math.max(depth, this.max_edge_depth.val);

                const width = EDGE_HORIZONTAL_SPACING * depth;

                const edge_element = div({ className: "quest_editor_EventSubGraphView_edge" });

                edge_element.style.left = `${4 - width}px`;
                edge_element.style.top = `${top}px`;
                edge_element.style.width = `${width}px`;
                edge_element.style.height = `${height}px`;

                // Add the child and parent id for debugging purposes.
                edge_element.dataset["parent"] = event.id.toString();
                edge_element.dataset["child"] = child.id.toString();

                this.edge_container_element.append(edge_element);
            }
        }
    };

    private create_event_element = (
        event: QuestEventModel,
        index: number,
    ): [HTMLElement, Disposable] => {
        const disposer = new Disposer();

        const event_view = disposer.add(new EventView(this.ctrl, event));

        this.event_gui_data.set(event, {
            event_view,
            position: index,
        });

        disposer.add({
            dispose: () => this.event_gui_data.delete(event),
        });

        return [event_view.element, disposer];
    };

    private after_events_changed = (change: ListChangeEvent<QuestEventModel>): void => {
        if (change.type === ListChangeType.ListChange) {
            this.sub_graph.val.forEach((event, i) => {
                this.event_gui_data.get(event)!.position = i;
            });

            this.update_edges();
        }
    };
}
