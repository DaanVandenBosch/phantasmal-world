import { ResizableWidget } from "../../core/gui/ResizableWidget";
import { bind_children_to, el } from "../../core/gui/dom";
import { quest_editor_store } from "../stores/QuestEditorStore";
import { QuestEventDagModel } from "../model/QuestEventDagModel";
import { Disposer } from "../../core/observable/Disposer";
import { NumberInput } from "../../core/gui/NumberInput";
import "./EventsView.css";
import { Disposable } from "../../core/observable/Disposable";
import { defer } from "lodash";

export class EventsView extends ResizableWidget {
    private readonly quest_disposer = this.disposable(new Disposer());

    readonly element = el.div({ class: "quest_editor_EventsView" });

    constructor() {
        super();

        this.disposables(
            quest_editor_store.current_quest.observe(this.update),
            quest_editor_store.current_area.observe(this.update),
        );

        this.finalize_construction(EventsView.prototype);
    }

    private update = (): void => {
        this.quest_disposer.dispose_all();

        const quest = quest_editor_store.current_quest.val;
        const area = quest_editor_store.current_area.val;

        if (quest && area) {
            this.quest_disposer.add(
                bind_children_to(
                    this.element,
                    quest.event_dags.filtered(dag => dag.area_id === area.id),
                    this.create_dag_element,
                ),
            );
        }
    };

    private create_dag_element = (dag: QuestEventDagModel): [HTMLElement, Disposable] => {
        const disposer = new Disposer();
        const element = el.div({ class: "quest_editor_EventsView_dag" });
        const event_elements = new Map<number, { element: HTMLDivElement; position: number }>();

        // Render events.
        dag.events.forEach((event, i) => {
            const event_element = el.div(
                { class: "quest_editor_EventsView_event" },
                el.table(
                    el.tr(el.th({ text: "ID:" }), el.td({ text: event.id.toString() })),
                    el.tr(
                        el.th({ text: "Section:" }),
                        el.td(
                            disposer.add(new NumberInput(event.section_id, { enabled: false }))
                                .element,
                        ),
                    ),
                    el.tr(el.th({ text: "Wave:" }), el.td({ text: event.wave.toString() })),
                    el.tr(
                        el.th({ text: "Delay:" }),
                        el.td(
                            disposer.add(new NumberInput(event.delay, { enabled: false })).element,
                        ),
                    ),
                ),
            );

            element.append(event_element);
            event_elements.set(event.id, { element: event_element, position: i });
        });

        // Render edges.
        defer(() => {
            const SPACING = 8;
            const used_depths: boolean[][] = Array(dag.events.length - 1);

            for (let i = 0; i < used_depths.length; i++) {
                used_depths[i] = [];
            }

            let max_depth = 0;

            for (const event of dag.events) {
                const { element: event_element, position } = event_elements.get(event.id)!;

                const y_offset = event_element.offsetTop + event_element.offsetHeight;

                for (const child of dag.get_children(event)) {
                    const { element: child_element, position: child_position } = event_elements.get(
                        child.id,
                    )!;
                    const child_y_offset = child_element.offsetTop;

                    const edge_element = el.div({ class: "quest_editor_EventsView_edge" });

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

                    element.append(edge_element);
                }
            }

            element.style.marginLeft = `${SPACING * max_depth}px`;
        });

        return [element, disposer];
    };
}
