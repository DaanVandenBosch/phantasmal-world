import { ResizableWidget } from "../../core/gui/ResizableWidget";
import { bind_children_to, el, icon, Icon } from "../../core/gui/dom";
import { quest_editor_store } from "../stores/QuestEditorStore";
import { QuestEventChainModel } from "../model/QuestEventChainModel";
import { Disposer } from "../../core/observable/Disposer";
import { NumberInput } from "../../core/gui/NumberInput";
import "./EventsView.css";
import { Button } from "../../core/gui/Button";
import { Disposable } from "../../core/observable/Disposable";

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
                    quest.event_chains.filtered(chain => chain.events.get(0).area_id === area.id),
                    this.create_chain_element,
                ),
            );
        }
    };

    private create_chain_element = (chain: QuestEventChainModel): [HTMLElement, Disposable] => {
        const disposer = new Disposer();
        const element = el.div(
            { class: "quest_editor_EventsView_chain" },
            ...chain.events.val.map(event =>
                el.div(
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
                                disposer.add(new NumberInput(event.delay, { enabled: false }))
                                    .element,
                            ),
                        ),
                    ),
                    el.div({ class: "quest_editor_EventsView_chain_arrow" }, icon(Icon.ArrowDown)),
                ),
            ),
            disposer.add(new Button("Add event", { icon_left: Icon.Plus, enabled: false })).element,
        );

        return [element, disposer];
    };
}
