import "./EventsView.css";
import { EventsController } from "../controllers/EventsController";
import { UnavailableView } from "./UnavailableView";
import { bind_attr, bind_children_to, disposable_listener, div } from "../../core/gui/dom";
import { Button } from "../../core/gui/Button";
import { ToolBar } from "../../core/gui/ToolBar";
import { EventSubGraphView } from "./EventSubGraphView";
import { property } from "../../core/observable";
import {
    ListChangeEvent,
    ListChangeType,
    ListProperty,
} from "../../core/observable/property/list/ListProperty";
import { QuestEventModel } from "../model/QuestEventModel";
import { ResizableView } from "../../core/gui/ResizableView";

export class EventsView extends ResizableView {
    private readonly sub_graph_views: Map<
        ListProperty<QuestEventModel>,
        EventSubGraphView
    > = new Map();
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
                this.add(new ToolBar((this.add_event_button = new Button({ text: "Add event" }))))
                    .element,
                (this.dag_container_element = div({
                    className: "quest_editor_EventsView_sub_graph_container",
                })),
            )),
            this.unavailable_view.element,
        );

        this.disposables(
            bind_attr(this.container_element, "hidden", ctrl.unavailable),
            this.unavailable_view.visible.bind_to(ctrl.unavailable),

            this.enabled.bind_to(ctrl.enabled),

            this.add_event_button.onclick.observe(ctrl.add_event),

            bind_children_to(
                this.dag_container_element,
                ctrl.event_sub_graphs,
                this.create_sub_graph_element,
                {
                    after: this.after_event_dags_changed,
                },
            ),

            disposable_listener(this.element, "focus", () => {
                ctrl.focused();
            }),

            disposable_listener(this.element, "click", () => {
                ctrl.set_selected_wave(undefined);
            }),
        );

        this.finalize_construction(EventsView);
    }

    dispose(): void {
        super.dispose();

        for (const dag_view of this.sub_graph_views.values()) {
            dag_view.dispose();
        }

        this.sub_graph_views.clear();
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

    protected set_enabled(enabled: boolean): void {
        super.set_enabled(enabled);

        for (const child of this.children) {
            child.enabled.val = enabled;
        }

        for (const dag_view of this.sub_graph_views.values()) {
            if (dag_view.element.parentNode) {
                dag_view.enabled.val = enabled;
            }
        }
    }

    private create_sub_graph_element = (sub_graph: ListProperty<QuestEventModel>): HTMLElement => {
        let sub_graph_view = this.sub_graph_views.get(sub_graph);

        if (!sub_graph_view) {
            sub_graph_view = new EventSubGraphView(
                this.ctrl,
                this.ctrl.event_dag.val!,
                sub_graph,
                this.max_edge_depth,
            );
            this.sub_graph_views.set(sub_graph, sub_graph_view);
        }

        return sub_graph_view.element;
    };

    private update_edges = (): void => {
        for (const dag_view of this.sub_graph_views.values()) {
            if (dag_view.element.parentNode) {
                dag_view.update_edges();
            }
        }
    };

    private after_event_dags_changed = (
        change: ListChangeEvent<ListProperty<QuestEventModel>>,
    ): void => {
        if (change.type === ListChangeType.ListChange) {
            for (const dag of change.inserted) {
                this.sub_graph_views.get(dag)?.update_edges();
            }
        }
    };
}
