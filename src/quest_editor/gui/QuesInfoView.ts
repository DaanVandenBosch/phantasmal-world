import { ResizableView } from "../../core/gui/ResizableView";
import { el } from "../../core/gui/dom";
import { quest_editor_store } from "../stores/QuestEditorStore";
import { Episode } from "../../core/data_formats/parsing/quest/Episode";
import { NumberInput } from "../../core/gui/NumberInput";
import { Disposer } from "../../core/observable/Disposer";
import { TextInput } from "../../core/gui/TextInput";
import { TextArea } from "../../core/gui/TextArea";
import "./QuesInfoView.css";
import { Label } from "../../core/gui/Label";

export class QuesInfoView extends ResizableView {
    readonly element = el.div({ class: "quest_editor_QuesInfoView" });

    private readonly table_element = el.table();
    private readonly episode_element: HTMLElement;
    private readonly id_input = this.disposable(new NumberInput());
    private readonly name_input = this.disposable(new TextInput());
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

    private readonly no_quest_element = el.div({ class: "quest_editor_QuesInfoView_no_quest" });
    private readonly no_quest_label = this.disposable(
        new Label("No quest loaded.", { enabled: false }),
    );

    private readonly quest_disposer = this.disposable(new Disposer());

    constructor() {
        super();

        const quest = quest_editor_store.current_quest;

        this.no_quest_element.append(this.no_quest_label.element);
        this.bind_hidden(this.no_quest_element, quest.map(q => q != undefined));

        this.table_element.append(
            el.tr({}, el.th({ text: "Episode:" }), (this.episode_element = el.td())),
            el.tr({}, el.th({ text: "ID:" }), el.td({}, this.id_input.element)),
            el.tr({}, el.th({ text: "Name:" }), el.td({}, this.name_input.element)),
            el.tr({}, el.th({ text: "Short description:", col_span: 2 })),
            el.tr({}, el.td({ col_span: 2 }, this.short_description_input.element)),
            el.tr({}, el.th({ text: "Long description:", col_span: 2 })),
            el.tr({}, el.td({ col_span: 2 }, this.long_description_input.element)),
        );
        this.bind_hidden(this.table_element, quest.map(q => q == undefined));

        this.element.append(this.table_element, this.no_quest_element);

        this.disposables(
            quest.observe(q => {
                this.quest_disposer.dispose();

                this.episode_element.textContent = q ? Episode[q.episode] : "";

                if (q) {
                    this.quest_disposer.add_all(
                        this.id_input.value.bind_bi(q.id),
                        this.name_input.value.bind_bi(q.name),
                        this.short_description_input.value.bind_bi(q.short_description),
                        this.long_description_input.value.bind_bi(q.long_description),
                    );
                }
            }),
        );
    }
}
