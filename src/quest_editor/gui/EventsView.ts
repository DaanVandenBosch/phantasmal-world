import { ResizableWidget } from "../../core/gui/ResizableWidget";
import { QuestEventDagModel } from "../model/QuestEventDagModel";
import "./EventsView.css";
import { EventsController } from "../controllers/EventsController";
import { UnavailableView } from "./UnavailableView";
import { bind_attr, bind_children_to, disposable_listener, div } from "../../core/gui/dom";
import { Button } from "../../core/gui/Button";
import { ToolBar } from "../../core/gui/ToolBar";
import { EventDagView } from "./EventDagView";
import { property } from "../../core/observable";
import { ListChangeEvent, ListChangeType } from "../../core/observable/property/list/ListProperty";

export class EventsView extends ResizableWidget {
    private readonly dag_views: Map<QuestEventDagModel, EventDagView> = new Map();
    private readonly dag_container_element: HTMLElement;
    private readonly container_element: HTMLElement;
    private readonly add_event_button: Button;
    private readonly unavailable_view = new UnavailableView("No quest loaded.");
    /**
     * The maximum amount of overlapping edges.
     */
    private readonly max_edge_depth = property(0);

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

        this.disposables(
            bind_attr(this.container_element, "hidden", ctrl.unavailable),
            this.unavailable_view.visible.bind_to(ctrl.unavailable),

            this.enabled.bind_to(ctrl.enabled),

            this.add_event_button.click.observe(ctrl.add_event),

            bind_children_to(this.dag_container_element, ctrl.event_dags, this.create_dag_element, {
                after: this.after_event_dags_changed,
            }),

            disposable_listener(this.element, "focus", () => {
                ctrl.focused();
            }),

            disposable_listener(this.element, "click", () => {
                ctrl.set_selected_wave(undefined);
            }),
        );

        this.finalize_construction();
    }

    dispose(): void {
        super.dispose();

        for (const dag_view of this.dag_views.values()) {
            dag_view.dispose();
        }

        this.dag_views.clear();
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

    private create_dag_element = (dag: QuestEventDagModel): HTMLElement => {
        let dag_view = this.dag_views.get(dag);

        if (!dag_view) {
            dag_view = new EventDagView(this.ctrl, dag, this.max_edge_depth);
            this.dag_views.set(dag, dag_view);
        }

        return dag_view.element;
    };

    private update_edges = (): void => {
        for (const dag_view of this.dag_views.values()) {
            if (dag_view.element.parentNode) {
                dag_view.update_edges();
            }
        }
    };

    private after_event_dags_changed = (change: ListChangeEvent<QuestEventDagModel>): void => {
        if (change.type === ListChangeType.ListChange) {
            for (const dag of change.inserted) {
                this.dag_views.get(dag)?.update_edges();
            }
        }
    };
}
