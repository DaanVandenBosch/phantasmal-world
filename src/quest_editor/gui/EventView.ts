import { Widget } from "../../core/gui/Widget";
import { bind_attr, disposable_listener, div, table, td, th, tr } from "../../core/gui/dom";
import { NumberInput } from "../../core/gui/NumberInput";
import { QuestEventModel } from "../model/QuestEventModel";
import "./EventView.css";
import { EventsController } from "../controllers/EventsController";

export class EventView extends Widget {
    private readonly delay_input: NumberInput;

    readonly element: HTMLElement;

    constructor(ctrl: EventsController, event: QuestEventModel) {
        super();

        const wave_node = document.createTextNode(event.wave.id.val.toString());
        this.delay_input = this.disposable(new NumberInput(event.delay.val, { min: 0, step: 1 }));

        this.element = div(
            { tabIndex: 0 },
            table(
                tr(th("ID:"), td(event.id.toString())),
                tr(th("Section:"), td(event.section_id.toString())),
                tr(th("Wave:"), td(wave_node as Node)),
                tr(th("Delay:"), td(this.delay_input.element)),
            ),
        );

        this.disposables(
            bind_attr(
                this.element,
                "className",
                ctrl
                    .is_selected(event)
                    .map(selected => `quest_editor_EventView ${selected ? "selected" : ""}`),
            ),

            event.wave.id.observe(({ value }) => (wave_node.data = value.toString())),

            disposable_listener(this.element, "focus", () => {
                ctrl.set_selected_wave(event.wave);
            }),

            disposable_listener(this.element, "click", e => {
                e.stopPropagation();
                ctrl.set_selected_wave(event.wave);
            }),

            disposable_listener(this.element, "keyup", evt => {
                if (evt.key === "Delete") {
                    ctrl.remove_event(event);
                }
            }),

            this.delay_input.value.bind_to(event.delay),
            this.delay_input.value.observe(e => ctrl.set_delay(event, e.value)),
        );

        this.finalize_construction();
    }

    protected set_enabled(enabled: boolean): void {
        super.set_enabled(enabled);
        this.delay_input.enabled.val = enabled;
    }
}
