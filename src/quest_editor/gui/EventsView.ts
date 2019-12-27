import { ResizableWidget } from "../../core/gui/ResizableWidget";
import { QuestEventDagModel } from "../model/QuestEventDagModel";
import { Disposer } from "../../core/observable/Disposer";
import "./EventsView.css";
import { EventsController } from "../controllers/EventsController";
import { UnavailableView } from "./UnavailableView";
import { bind_attr, bind_children_to, div, Icon, table, td, th, tr } from "../../core/gui/dom";
import { NumberInput } from "../../core/gui/NumberInput";
import { QuestEventModel } from "../model/QuestEventModel";
import { Disposable } from "../../core/observable/Disposable";
import { Button } from "../../core/gui/Button";
import { ToolBar } from "../../core/gui/ToolBar";

type DagGuiData = {
    dag: QuestEventDagModel;
    element: HTMLElement;
    edge_container_element: HTMLElement;
    /**
     * Maps event IDs to GUI data.
     */
    event_gui_data: Map<number, { element: HTMLDivElement; position: number }>;
};

export class EventsView extends ResizableWidget {
    private readonly dag_gui_data: DagGuiData[] = [];

    private readonly dag_container_element: HTMLElement;
    private readonly container_element: HTMLElement;
    private readonly add_event_button: Button;
    private readonly unavailable_view = new UnavailableView("No quest loaded.");

    readonly element: HTMLElement;

    constructor(private readonly ctrl: EventsController) {
        super();

        this.element = div(
            { className: "quest_editor_EventsView", tabIndex: -1 },
            (this.container_element = div(
                { className: "quest_editor_EventsView_container" },
                this.disposable(
                    new ToolBar((this.add_event_button = new Button({ text: "Add event" }))),
                ).element,
                (this.dag_container_element = div({
                    className: "quest_editor_EventsView_dag_container",
                })),
            )),
            this.unavailable_view.element,
        );

        this.element.addEventListener("focus", ctrl.focused, true);

        this.disposables(
            bind_attr(this.container_element, "hidden", ctrl.unavailable),
            this.unavailable_view.visible.bind_to(ctrl.unavailable),

            this.enabled.bind_to(ctrl.enabled),

            this.add_event_button.click.observe(ctrl.add_event),

            bind_children_to(this.dag_container_element, ctrl.event_dags, this.create_dag_element, {
                after: this.update_edges,
            }),
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

    private create_dag_element = (
        dag: QuestEventDagModel,
        index: number,
    ): [HTMLElement, Disposable] => {
        const event_container_element = div({
            className: "quest_editor_EventsView_event_container",
        });
        const edge_container_element = div({
            className: "quest_editor_EventsView_edge_container",
        });
        const dag_element = div(
            { className: "quest_editor_EventsView_dag" },
            edge_container_element,
            event_container_element,
        );

        const gui_data = (this.dag_gui_data[index] = {
            dag,
            element: dag_element,
            edge_container_element,
            event_gui_data: new Map<number, { element: HTMLDivElement; position: number }>(),
        });

        const disposer = new Disposer(
            bind_children_to(
                event_container_element,
                dag.events,
                this.create_event_element(gui_data),
            ),
            {
                dispose: () => {
                    this.dag_gui_data.splice(index, 1);
                },
            },
        );

        return [dag_element, disposer];
    };

    private create_event_element = (gui_data: DagGuiData) => (
        event: QuestEventModel,
        index: number,
    ): [HTMLElement, Disposer] => {
        const disposer = new Disposer();

        const wave_node = document.createTextNode(event.wave.id.val.toString());
        const wave_button = disposer.add(new Button({ icon_left: Icon.Eye }));
        const delay_input = disposer.add(new NumberInput(event.delay.val, { min: 0, step: 1 }));

        disposer.add_all(
            event.wave.id.observe(({ value }) => (wave_node.data = value.toString())),
            wave_button.click.observe(() => this.ctrl.toggle_current_wave(event.wave)),

            delay_input.value.bind_to(event.delay),
            delay_input.value.observe(e => this.ctrl.set_delay(event, e.value)),
            delay_input.enabled.bind_to(this.ctrl.enabled),
        );

        const event_element = div(
            { className: "quest_editor_EventsView_event" },
            table(
                tr(th("ID:"), td(event.id.toString())),
                tr(th("Section:"), td(event.section_id.toString())),
                tr(th("Wave:"), td(wave_node as Node, " ", wave_button.element)),
                tr(th("Delay:"), td(delay_input.element)),
            ),
        );

        gui_data.event_gui_data.set(event.id, { element: event_element, position: index });

        return [event_element, disposer];
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

            const used_depths: boolean[][] = Array(dag.events.length.val - 1);

            for (let i = 0; i < used_depths.length; i++) {
                used_depths[i] = [];
            }

            for (const event of dag.events.val) {
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
