import { ResizableWidget } from "../../core/gui/ResizableWidget";
import { QuestEventDagModel } from "../model/QuestEventDagModel";
import { Disposer } from "../../core/observable/Disposer";
import "./EventsView.css";
import { EventsController } from "../controllers/EventsController";
import { UnavailableView } from "./UnavailableView";
import { bind_attr, div, table, td, th, tr } from "../../core/gui/dom";
import { ListChangeEvent, ListChangeType } from "../../core/observable/property/list/ListProperty";
import { defer } from "lodash";
import { NumberInput } from "../../core/gui/NumberInput";

type DagGuiData = {
    dag: QuestEventDagModel;
    element: HTMLElement;
    edge_container_element: HTMLElement;
    disposer: Disposer;
    /**
     * Maps event IDs to GUI data.
     */
    event_gui_data: Map<number, { element: HTMLDivElement; position: number }>;
};

export class EventsView extends ResizableWidget {
    private readonly dag_gui_data: DagGuiData[] = [];

    private readonly container_element = div({ className: "quest_editor_EventsView_container" });
    private readonly unavailable_view = new UnavailableView("No quest loaded.");

    readonly element = div(
        { className: "quest_editor_EventsView", tabIndex: -1 },
        this.container_element,
        this.unavailable_view.element,
    );

    constructor(private readonly ctrl: EventsController) {
        super();

        this.element.addEventListener("focus", ctrl.focused, true);

        this.disposables(
            bind_attr(this.container_element, "hidden", ctrl.unavailable),
            this.unavailable_view.visible.bind_to(ctrl.unavailable),

            this.enabled.bind_to(ctrl.enabled),

            ctrl.event_dags.observe_list(this.observe_event_dags),
        );

        this.finalize_construction();
    }

    resize(width: number, height: number): this {
        super.resize(width, height);
        this.update_edges();
        return this;
    }

    focus(): void {
        super.focus();
        this.update_edges();
    }

    dispose(): void {
        super.dispose();

        for (const { disposer } of this.dag_gui_data) {
            disposer.dispose();
        }
    }

    private observe_event_dags = (change: ListChangeEvent<QuestEventDagModel>): void => {
        if (change.type === ListChangeType.ListChange) {
            for (const removed of this.dag_gui_data.splice(change.index, change.removed.length)) {
                removed.element.remove();
                removed.disposer.dispose();
            }

            let index = change.index;

            for (const dag of change.inserted) {
                const data = this.create_dag_ui_data(dag);
                this.dag_gui_data.splice(index, 0, data);
                this.container_element.insertBefore(
                    data.element,
                    this.container_element.children.item(index),
                );

                index++;
            }

            defer(this.update_edges);
        }
    };

    private create_dag_ui_data = (dag: QuestEventDagModel): DagGuiData => {
        const disposer = new Disposer();
        const event_gui_data = new Map<number, { element: HTMLDivElement; position: number }>();

        const element = div({ className: "quest_editor_EventsView_dag" });

        const edge_container_element = div({
            className: "quest_editor_EventsView_edge_container",
        });
        element.append(edge_container_element);

        dag.events.forEach((event, i) => {
            const section_id_input = disposer.add(new NumberInput(event.section_id.val));

            const delay_input = disposer.add(new NumberInput(event.delay.val));

            disposer.add_all(
                section_id_input.value.bind_to(event.section_id),
                section_id_input.value.observe(e => this.ctrl.set_section_id(event, e.value)),
                section_id_input.enabled.bind_to(this.ctrl.enabled),

                delay_input.value.bind_to(event.delay),
                delay_input.value.observe(e => this.ctrl.set_delay(event, e.value)),
                delay_input.enabled.bind_to(this.ctrl.enabled),
            );

            const event_element = div(
                { className: "quest_editor_EventsView_event" },
                table(
                    tr(th("ID:"), td(event.id.toString())),
                    tr(th("Section:"), td(section_id_input.element)),
                    tr(th("Wave:"), td(event.wave.toString())),
                    tr(th("Delay:"), td(delay_input.element)),
                ),
            );

            element.append(event_element);
            event_gui_data.set(event.id, { element: event_element, position: i });
        });

        return {
            dag,
            element,
            edge_container_element,
            disposer,
            event_gui_data,
        };
    };

    /**
     * This method does measurements of the event elements. So it should be called after the event
     * elements have been added to the DOM and have been laid out by the browser.
     */
    private update_edges = (): void => {
        const SPACING = 8;
        let max_depth = 0;

        for (const { dag, edge_container_element, event_gui_data } of this.dag_gui_data) {
            edge_container_element.innerHTML = "";

            const used_depths: boolean[][] = Array(dag.events.length - 1);

            for (let i = 0; i < used_depths.length; i++) {
                used_depths[i] = [];
            }

            for (const event of dag.events) {
                const { element: event_element, position } = event_gui_data.get(event.id)!;

                const y_offset = event_element.offsetTop + event_element.offsetHeight;

                for (const child of dag.get_children(event)) {
                    const { element: child_element, position: child_position } = event_gui_data.get(
                        child.id,
                    )!;
                    const child_y_offset = child_element.offsetTop;

                    const edge_element = div({ className: "quest_editor_EventsView_edge" });

                    const top = Math.min(y_offset, child_y_offset) - 20;
                    const height = Math.max(y_offset, child_y_offset) - top + 20;

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

                    max_depth = Math.max(depth, max_depth);

                    const width = SPACING * depth;

                    edge_element.style.left = `${4 - width}px`;
                    edge_element.style.top = `${top}px`;
                    edge_element.style.width = `${width}px`;
                    edge_element.style.height = `${height}px`;

                    edge_container_element.append(edge_element);
                }
            }
        }

        for (const { element } of this.dag_gui_data) {
            element.style.marginLeft = `${SPACING * max_depth}px`;
        }
    };
}
