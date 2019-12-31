import { Widget } from "../../core/gui/Widget";
import {
    bind_attr,
    bind_children_to,
    disposable_listener,
    div,
    span,
    table,
    td,
    th,
    tr,
} from "../../core/gui/dom";
import { NumberInput } from "../../core/gui/NumberInput";
import { QuestEventModel } from "../model/QuestEventModel";
import "./EventView.css";
import { EventsController } from "../controllers/EventsController";
import {
    QuestEventActionLockModel,
    QuestEventActionModel,
    QuestEventActionSpawnNpcsModel,
    QuestEventActionUnlockModel,
} from "../model/QuestEventActionModel";
import { Disposer } from "../../core/observable/Disposer";
import { property } from "../../core/observable";

export class EventView extends Widget {
    private readonly inputs_enabled = property(true);
    private readonly delay_input: NumberInput;

    readonly element: HTMLElement;

    constructor(ctrl: EventsController, event: QuestEventModel) {
        super();

        const wave_node = document.createTextNode(event.wave.id.val.toString());
        this.delay_input = this.disposable(
            new NumberInput(event.delay.val, { min: 0, step: 1, enabled: this.inputs_enabled }),
        );
        const action_table = table({ className: "quest_editor_EventView_actions" });

        this.element = div(
            { tabIndex: 0 },
            table(
                tr(th("ID:"), td(event.id.toString())),
                tr(th("Section:"), td(event.section_id.toString())),
                tr(th("Wave:"), td(wave_node as Node)),
                tr(th("Delay:"), td(this.delay_input.element)),
                tr(th({ colSpan: 2 }, "Actions:")),
                tr(td({ colSpan: 2 }, action_table)),
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

            bind_children_to(action_table, event.actions, this.create_action_element),
        );

        this.finalize_construction();
    }

    protected set_enabled(enabled: boolean): void {
        super.set_enabled(enabled);
        this.inputs_enabled.val = enabled;
    }

    private create_action_element = (
        action: QuestEventActionModel,
    ): [HTMLTableRowElement, Disposer] => {
        const disposer = new Disposer();
        let label: string;
        let node: Node;

        if (action instanceof QuestEventActionSpawnNpcsModel) {
            label = "Spawn:";
            node = span(
                disposer.add(
                    new NumberInput(action.section_id, { min: 0, step: 1, enabled: false }),
                ).element,
                disposer.add(
                    new NumberInput(action.appear_flag, { min: 0, step: 1, enabled: false }),
                ).element,
            );
        } else if (
            action instanceof QuestEventActionUnlockModel ||
            action instanceof QuestEventActionLockModel
        ) {
            label = action instanceof QuestEventActionUnlockModel ? "Unlock:" : "Lock:";
            const input = disposer.add(
                new NumberInput(action.door_id.val, {
                    min: 0,
                    step: 1,
                    enabled: this.inputs_enabled,
                }),
            );
            node = input.element;

            disposer.add_all(
                input.value.bind_to(action.door_id),
                input.value.observe(({ value }) => action.set_door_id(value)),
            );
        } else {
            throw new Error(`Unsupported action type ${action.constructor.name}.`);
        }

        return [tr(th(label), td(node)), disposer];
    };
}
