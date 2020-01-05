import {
    bind_attr,
    bind_children_to,
    disposable_listener,
    div,
    Icon,
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
    QuestEventActionModel,
    QuestEventActionType,
    QuestEventActionTypes,
} from "../model/QuestEventActionModel";
import { Disposer } from "../../core/observable/Disposer";
import { property } from "../../core/observable";
import { DropDown } from "../../core/gui/DropDown";
import { Button } from "../../core/gui/Button";
import { View } from "../../core/gui/View";

export class EventView extends View {
    private readonly inputs_enabled = property(true);
    private readonly delay_input: NumberInput;

    readonly element: HTMLElement;

    constructor(private readonly ctrl: EventsController, private readonly event: QuestEventModel) {
        super();

        const wave_node = document.createTextNode(event.wave.id.val.toString());
        this.delay_input = this.add(
            new NumberInput(event.delay.val, { min: 0, step: 1, enabled: this.inputs_enabled }),
        );
        const action_table = table({ className: "quest_editor_EventView_actions" });
        const add_action_dropdown: DropDown<QuestEventActionType> = this.add(
            new DropDown({
                text: "Add action",
                items: QuestEventActionTypes,
                to_label(type: QuestEventActionType): string {
                    switch (type) {
                        case QuestEventActionType.SpawnNpcs:
                            return "Spawn NPCs";
                        case QuestEventActionType.Unlock:
                            return "Unlock door";
                        case QuestEventActionType.Lock:
                            return "Lock door";
                    }
                },
            }),
        );

        this.element = div(
            { tabIndex: 0 },
            table(
                tr(th("ID:"), td(event.id.toString())),
                tr(th("Section:"), td(event.section_id.toString())),
                tr(th("Wave:"), td(wave_node as Node)),
                tr(th("Delay:"), td(this.delay_input.element)),
                tr(th({ colSpan: 2 }, "Actions:")),
                tr(td({ colSpan: 2 }, action_table)),
                tr(td({ colSpan: 2 }, add_action_dropdown.element)),
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

            add_action_dropdown.chosen.observe(({ value }) => ctrl.add_action(event, value)),
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

        if (action.type === QuestEventActionType.SpawnNpcs) {
            label = "Spawn:";
            node = div(
                div(
                    disposer.add(
                        new NumberInput(action.section_id, { min: 0, step: 1, enabled: false }),
                    ).element,
                ),
                div(
                    disposer.add(
                        new NumberInput(action.appear_flag, { min: 0, step: 1, enabled: false }),
                    ).element,
                ),
            );
        } else {
            label = action.type === QuestEventActionType.Unlock ? "Unlock:" : "Lock:";
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
        }

        const remove_button = disposer.add(new Button({ icon_left: Icon.Remove }));

        disposer.add_all(
            remove_button.click.observe(() => this.ctrl.remove_action(this.event, action)),
        );

        return [tr(th(label), td(node), td(remove_button.element)), disposer];
    };
}
