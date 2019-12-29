import { Widget } from "../../core/gui/Widget";
import { bind_children_to, div } from "../../core/gui/dom";
import { QuestEventDagModel } from "../model/QuestEventDagModel";
import { QuestEventModel } from "../model/QuestEventModel";
import { EventView } from "./EventView";
import { EventsController } from "../controllers/EventsController";
import { Disposable } from "../../core/observable/Disposable";
import "./EventDagView.css";
import { Disposer } from "../../core/observable/Disposer";
import { ListChangeEvent, ListChangeType } from "../../core/observable/property/list/ListProperty";
import { WritableProperty } from "../../core/observable/property/WritableProperty";

const EDGE_HORIZONTAL_SPACING = 8;
const EDGE_VERTICAL_SPACING = 20;

export class EventDagView extends Widget {
    /**
     * Maps event IDs to GUI data.
     */
    private readonly event_gui_data: Map<
        QuestEventModel,
        { element: HTMLElement; position: number }
    > = new Map();

    private readonly event_container_element = div({
        className: "quest_editor_EventDagView_event_container",
    });

    private readonly edge_container_element = div({
        className: "quest_editor_EventDagView_edge_container",
    });

    readonly element = div(
        { className: "quest_editor_EventDagView" },
        this.edge_container_element,
        this.event_container_element,
    );

    constructor(
        private readonly ctrl: EventsController,
        private readonly dag: QuestEventDagModel,
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

            bind_children_to(this.event_container_element, dag.events, this.create_event_element, {
                after: this.after_events_changed,
            }),
        );

        this.finalize_construction();
    }

    /**
     * This method does measurements of the event elements. So it should be called after the event
     * elements have been added to the DOM and have been laid out by the browser.
     */
    update_edges = (): void => {
        this.edge_container_element.innerHTML = "";

        if (this.dag.events.length.val === 0) return;

        // Each edge has a different depth (higher depth means further distance from event nodes).
        // Keep track of the used depths here, to ensure edges never overlap.
        const used_depths: boolean[][] = Array(this.dag.events.length.val - 1);

        for (let i = 0; i < used_depths.length; i++) {
            used_depths[i] = [];
        }

        for (const event of this.dag.events.val) {
            const { element: event_element, position } = this.event_gui_data.get(event)!;

            const y_offset = event_element.offsetTop + event_element.offsetHeight;

            for (const child of this.dag.get_children(event)) {
                const {
                    element: child_element,
                    position: child_position,
                } = this.event_gui_data.get(child)!;
                const child_y_offset = child_element.offsetTop;

                const edge_element = div({ className: "quest_editor_EventsView_edge" });

                const top = Math.min(y_offset, child_y_offset) - EDGE_VERTICAL_SPACING;
                const height = Math.max(y_offset, child_y_offset) - top + EDGE_VERTICAL_SPACING;

                let depth = 1;
                const low_pos = Math.min(position, child_position);
                const high_pos = Math.max(position, child_position);

                outer: while (true) {
                    for (let i = low_pos; i < high_pos; i++) {
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

                edge_element.style.left = `${4 - width}px`;
                edge_element.style.top = `${top}px`;
                edge_element.style.width = `${width}px`;
                edge_element.style.height = `${height}px`;

                this.edge_container_element.append(edge_element);
            }
        }
    };

    private create_event_element = (
        event: QuestEventModel,
        index: number,
    ): [HTMLElement, Disposable] => {
        const disposer = new Disposer();

        const event_view = disposer.add(new EventView(this.ctrl, this.dag, event));

        this.event_gui_data.set(event, {
            element: event_view.element,
            position: index,
        });

        disposer.add({
            dispose: () => this.event_gui_data.delete(event),
        });

        return [event_view.element, disposer];
    };

    private after_events_changed = (change: ListChangeEvent<QuestEventModel>): void => {
        if (change.type === ListChangeType.ListChange) {
            const data = [...this.event_gui_data.values()];

            for (let i = change.index + change.inserted.length; i < data.length; i++) {
                data[i].position = i;
            }

            this.update_edges();
        }
    };
}
