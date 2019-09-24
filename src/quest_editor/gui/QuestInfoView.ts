import { ResizableWidget } from "../../core/gui/ResizableWidget";
import { el } from "../../core/gui/dom";
import { quest_editor_store } from "../stores/QuestEditorStore";
import { Episode } from "../../core/data_formats/parsing/quest/Episode";
import { NumberInput } from "../../core/gui/NumberInput";
import { Disposer } from "../../core/observable/Disposer";
import { TextInput } from "../../core/gui/TextInput";
import { TextArea } from "../../core/gui/TextArea";
import "./QuestInfoView.css";
import { DisabledView } from "./DisabledView";

export class QuestInfoView extends ResizableWidget {
    readonly element = el.div({ class: "quest_editor_QuestInfoView", tab_index: -1 });

    private readonly table_element = el.table();
    private readonly episode_element: HTMLElement;
    private readonly id_input = this.disposable(new NumberInput());
    private readonly name_input = this.disposable(
        new TextInput("", {
            max_length: 32,
        }),
    );
    private readonly short_description_input = this.disposable(
        new TextArea("", {
            max_length: 128,
            font_family: '"Courier New", monospace',
            cols: 25,
            rows: 5,
        }),
    );
    private readonly long_description_input = this.disposable(
        new TextArea("", {
            max_length: 288,
            font_family: '"Courier New", monospace',
            cols: 25,
            rows: 10,
        }),
    );

    private readonly no_quest_view = new DisabledView("No quest loaded.");

    private readonly quest_disposer = this.disposable(new Disposer());

    constructor() {
        super();

        const quest = quest_editor_store.current_quest;
        const no_quest = quest.map(q => q == undefined);

        this.table_element.append(
            el.tr({}, el.th({ text: "Episode:" }), (this.episode_element = el.td())),
            el.tr({}, el.th({ text: "ID:" }), el.td({}, this.id_input.element)),
            el.tr({}, el.th({ text: "Name:" }), el.td({}, this.name_input.element)),
            el.tr({}, el.th({ text: "Short description:", col_span: 2 })),
            el.tr({}, el.td({ col_span: 2 }, this.short_description_input.element)),
            el.tr({}, el.th({ text: "Long description:", col_span: 2 })),
            el.tr({}, el.td({ col_span: 2 }, this.long_description_input.element)),
        );

        this.bind_hidden(this.table_element, no_quest);

        this.element.append(this.table_element, this.no_quest_view.element);

        this.element.addEventListener("focus", () => quest_editor_store.undo.make_current(), true);

        this.disposables(
            this.no_quest_view.visible.bind_to(no_quest),

            quest.observe(({ value: q }) => {
                this.quest_disposer.dispose_all();

                this.episode_element.textContent = q ? Episode[q.episode] : "";

                if (q) {
                    this.quest_disposer.add_all(
                        this.id_input.value.bind_to(q.id),
                        this.id_input.value.observe(quest_editor_store.id_changed),

                        this.name_input.value.bind_to(q.name),
                        this.name_input.value.observe(quest_editor_store.name_changed),

                        this.short_description_input.value.bind_to(q.short_description),
                        this.short_description_input.value.observe(
                            quest_editor_store.short_description_changed,
                        ),

                        this.long_description_input.value.bind_to(q.long_description),
                        this.long_description_input.value.observe(
                            quest_editor_store.long_description_changed,
                        ),
                    );
                }
            }),
        );

        this.finalize_construction(QuestInfoView.prototype);
    }
}
