import { ResizableView } from "../../../core/gui/ResizableView";
import { div, table, td, tr } from "../../../core/gui/dom";
import { Select } from "../../../core/gui/Select";
import { CharacterClassOptionsController } from "../../controllers/model/CharacterClassOptionsController";
import "./CharacterClassOptionsView.css";
import { SectionId, SectionIds } from "../../../core/model";

export class CharacterClassOptionsView extends ResizableView {
    readonly element: HTMLElement;

    constructor(ctrl: CharacterClassOptionsController) {
        super();

        const section_id_select: Select<SectionId | undefined> = this.add(
            new Select({
                class: "viewer_model_CharacterClassOptionsView_section_id",
                label: "Section ID:",
                items: SectionIds,
                selected: ctrl.current_section_id,
                to_label: section_id => (section_id == undefined ? "" : SectionId[section_id]),
                enabled: ctrl.enabled,
            }),
        );

        const body_select: Select<number | undefined> = this.add(
            new Select({
                class: "viewer_model_CharacterClassOptionsView_body",
                label: "Body:",
                items: ctrl.current_body_options,
                selected: ctrl.current_body,
                enabled: ctrl.enabled,
            }),
        );

        this.element = div(
            { className: "viewer_model_CharacterClassOptionsView" },
            table(
                tr(td(section_id_select.label?.element), td(section_id_select.element)),
                tr(td(body_select.label?.element), td(body_select.element)),
            ),
        );

        this.disposables(
            section_id_select.selected.observe(({ value }) => ctrl.set_current_section_id(value)),
            body_select.selected.observe(({ value }) => ctrl.set_current_body(value)),
        );

        this.finalize_construction();
    }
}
