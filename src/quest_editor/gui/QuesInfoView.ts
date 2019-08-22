import { ResizableView } from "../../core/gui/ResizableView";
import { el } from "../../core/gui/dom";
import { quest_editor_store } from "../stores/QuestEditorStore";
import { Episode } from "../../core/data_formats/parsing/quest/Episode";

export class QuesInfoView extends ResizableView {
    readonly element = el("div", { class: "quest_editor_QuesInfoView" });

    private readonly table_element = el("table");
    private readonly episode_element: HTMLElement;
    private readonly id_element: HTMLElement;
    private readonly name_element: HTMLElement;

    constructor() {
        super();

        const quest = quest_editor_store.current_quest;

        this.bind_hidden(this.table_element, quest.map(q => q == undefined));

        this.table_element.append(
            el("tr", {}, el("th", { text: "Episode:" }), (this.episode_element = el("td"))),
            el("tr", {}, el("th", { text: "ID:" }), (this.id_element = el("td"))),
            el("tr", {}, el("th", { text: "Name:" }), (this.name_element = el("td"))),
        );

        this.element.append(this.table_element);

        this.disposables(
            quest.observe(q => {
                if (q) {
                    this.episode_element.textContent = Episode[q.episode];
                    this.id_element.textContent = q.id.val.toString();
                    this.name_element.textContent = q.name.val;
                }
            }),
        );
    }
}
