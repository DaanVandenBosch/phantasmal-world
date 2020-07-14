import { Episode } from "../../core/data_formats/parsing/quest/Episode";
import { NumberInput } from "../../core/gui/NumberInput";
import { Disposer } from "../../core/observable/Disposer";
import { TextInput } from "../../core/gui/TextInput";
import { TextArea } from "../../core/gui/TextArea";
import "./QuestInfoView.css";
import { UnavailableView } from "./UnavailableView";
import { QuestInfoController } from "../controllers/QuestInfoController";
import { bind_attr, div, table, td, th, tr } from "../../core/gui/dom";
import { ResizableView } from "../../core/gui/ResizableView";

export class QuestInfoView extends ResizableView {
    readonly element = div({ className: "quest_editor_QuestInfoView", tabIndex: -1 });

    private readonly table_element = table();
    private readonly episode_element: HTMLElement;
    private readonly id_input = this.add(new NumberInput(0, { min: 0, step: 1 }));
    private readonly name_input = this.add(
        new TextInput("", {
            max_length: 32,
        }),
    );
    private readonly short_description_input = this.add(
        new TextArea("", {
            max_length: 128,
            font_family: '"Courier New", monospace',
            cols: 25,
            rows: 5,
        }),
    );
    private readonly long_description_input = this.add(
        new TextArea("", {
            max_length: 288,
            font_family: '"Courier New", monospace',
            cols: 25,
            rows: 10,
        }),
    );

    private readonly unavailable_view = this.add(new UnavailableView("No quest loaded."));

    private readonly quest_disposer = this.disposable(new Disposer());

    constructor(ctrl: QuestInfoController) {
        super();

        const quest = ctrl.current_quest;

        this.table_element.append(
            tr(th("Episode:"), (this.episode_element = td())),
            tr(th("ID:"), td(this.id_input.element)),
            tr(th("Name:"), td(this.name_input.element)),
            tr(th({ colSpan: 2 }, "Short description:")),
            tr(td({ colSpan: 2 }, this.short_description_input.element)),
            tr(th({ colSpan: 2 }, "Long description:")),
            tr(td({ colSpan: 2 }, this.long_description_input.element)),
        );

        this.element.append(this.table_element, this.unavailable_view.element);

        this.element.addEventListener("focus", ctrl.focused, true);

        this.disposables(
            this.unavailable_view.visible.bind_to(ctrl.unavailable),

            bind_attr(this.table_element, "hidden", ctrl.unavailable),

            quest.observe(({ value: q }) => {
                this.quest_disposer.dispose_all();

                this.episode_element.textContent = q ? Episode[q.episode] : "";

                if (q) {
                    this.quest_disposer.add_all(
                        this.id_input.value.bind_to(q.id),
                        this.id_input.value.observe(({ value }) => ctrl.set_id(value)),

                        this.name_input.value.bind_to(q.name),
                        this.name_input.value.observe(({ value }) => ctrl.set_name(value)),

                        this.short_description_input.value.bind_to(q.short_description),
                        this.short_description_input.value.observe(({ value }) =>
                            ctrl.set_short_description(value),
                        ),

                        this.long_description_input.value.bind_to(q.long_description),
                        this.long_description_input.value.observe(({ value }) =>
                            ctrl.set_long_description(value),
                        ),

                        this.enabled.bind_to(ctrl.enabled),
                    );
                }
            }),
        );

        this.finalize_construction(QuestInfoView);
    }

    protected set_enabled(enabled: boolean): void {
        super.set_enabled(enabled);

        this.id_input.enabled.val = enabled;
        this.name_input.enabled.val = enabled;
        this.short_description_input.enabled.val = enabled;
        this.long_description_input.enabled.val = enabled;
    }
}
