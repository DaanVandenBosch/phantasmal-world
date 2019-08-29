import { ToolBar } from "../../core/gui/ToolBar";
import { FileButton } from "../../core/gui/FileButton";
import { Button } from "../../core/gui/Button";
import { quest_editor_store } from "../stores/QuestEditorStore";
import { undo_manager } from "../../core/undo/UndoManager";
import { Select } from "../../core/gui/Select";
import { array_property } from "../../core/observable";
import { AreaModel } from "../model/AreaModel";
import { Icon } from "../../core/gui/dom";

export class QuestEditorToolBar extends ToolBar {
    constructor() {
        const open_file_button = new FileButton("Open file...", {
            icon_left: Icon.File,
            accept: ".qst",
        });
        const save_as_button = new Button("Save as...", { icon_left: Icon.Save });
        const undo_button = new Button("Undo", {
            icon_left: Icon.Undo,
            tooltip: undo_manager.first_undo.map(action =>
                action ? `Undo "${action.description}"` : "Nothing to undo",
            ),
        });
        const redo_button = new Button("Redo", {
            icon_left: Icon.Redo,
            tooltip: undo_manager.first_redo.map(action =>
                action ? `Redo "${action.description}"` : "Nothing to redo",
            ),
        });
        const area_select = new Select<AreaModel>(
            quest_editor_store.current_quest.flat_map(quest => {
                if (quest) {
                    return quest.area_variants.map(variants =>
                        variants.map(variant => variant.area),
                    );
                } else {
                    return array_property<AreaModel>();
                }
            }),
            element => element.name,
        );

        super({
            children: [open_file_button, save_as_button, undo_button, redo_button, area_select],
        });

        const quest_loaded = quest_editor_store.current_quest.map(q => q != undefined);

        this.disposables(
            open_file_button.files.observe(({ value: files }) => {
                if (files.length) {
                    quest_editor_store.open_file(files[0]);
                }
            }),

            save_as_button.enabled.bind_to(quest_loaded),

            undo_button.enabled.bind_to(undo_manager.can_undo),
            undo_button.click.observe(() => undo_manager.undo()),

            redo_button.enabled.bind_to(undo_manager.can_redo),
            redo_button.click.observe(() => undo_manager.redo()),

            area_select.enabled.bind_to(quest_loaded),
            area_select.selected.bind_to(quest_editor_store.current_area),
            area_select.selected.observe(({ value: area }) =>
                quest_editor_store.set_current_area(area),
            ),
        );
    }
}
