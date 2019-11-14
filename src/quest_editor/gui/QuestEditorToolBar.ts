import { ToolBar } from "../../core/gui/ToolBar";
import { FileButton } from "../../core/gui/FileButton";
import { Button } from "../../core/gui/Button";
import { quest_editor_store } from "../stores/QuestEditorStore";
import { undo_manager } from "../../core/undo/UndoManager";
import { Select } from "../../core/gui/Select";
import { list_property } from "../../core/observable";
import { AreaModel } from "../model/AreaModel";
import { Icon } from "../../core/gui/dom";
import { DropDown } from "../../core/gui/DropDown";
import { Episode } from "../../core/data_formats/parsing/quest/Episode";
import { area_store } from "../stores/AreaStore";
import { gui_store, GuiTool } from "../../core/stores/GuiStore";

export class QuestEditorToolBar extends ToolBar {
    constructor() {
        const new_quest_button = new DropDown(
            "New quest",
            [Episode.I],
            episode => `Episode ${Episode[episode]}`,
            {
                icon_left: Icon.NewFile,
            },
        );
        const open_file_button = new FileButton("Open file...", {
            icon_left: Icon.File,
            accept: ".qst",
            tooltip: "Open a quest file (Ctrl-O)",
        });
        const save_as_button = new Button("Save as...", {
            icon_left: Icon.Save,
            tooltip: "Save this quest to new file (Ctrl-Shift-S)",
        });
        const undo_button = new Button("Undo", {
            icon_left: Icon.Undo,
            tooltip: undo_manager.first_undo.map(
                action =>
                    (action ? `Undo "${action.description}"` : "Nothing to undo") + " (Ctrl-Z)",
            ),
        });
        const redo_button = new Button("Redo", {
            icon_left: Icon.Redo,
            tooltip: undo_manager.first_redo.map(
                action =>
                    (action ? `Redo "${action.description}"` : "Nothing to redo") +
                    " (Ctrl-Shift-Z)",
            ),
        });
        // TODO: make sure select menu is updated when entity counts change.
        const area_select = new Select<AreaModel>(
            quest_editor_store.current_quest.flat_map(quest => {
                if (quest) {
                    return list_property(
                        undefined,
                        ...area_store.get_areas_for_episode(quest.episode),
                    );
                } else {
                    return list_property<AreaModel>();
                }
            }),
            area => {
                const quest = quest_editor_store.current_quest.val;

                if (quest) {
                    const entity_count = quest.entities_per_area.val.get(area.id);
                    return area.name + (entity_count ? ` (${entity_count})` : "");
                } else {
                    return area.name;
                }
            },
        );
        const run_button = new Button("Run in VM", {
            icon_left: Icon.Play,
            tooltip: "[Experimental] Run the current quest in a virtual machine (F5)",
        });
        const resume_button = new Button("Resume", {
            icon_left: Icon.SquareArrowRight,
            tooltip: "Resume execution"
        });
        const step_over_button = new Button("Step over", {
            icon_left: Icon.LongArrowRight,
            tooltip: "Execute the next line and step over any function calls",
        });
        const step_in_button = new Button("Step in", {
            icon_left: Icon.LevelDown,
            tooltip: "Execute the next line and step inside any function calls"
        });
        const step_out_button = new Button("Step out", {
            icon_left: Icon.LevelUp,
            tooltip: "Execute until outside of current call frame"
        });

        const children = [
            new_quest_button,
            open_file_button,
            save_as_button,
            undo_button,
            redo_button,
            area_select,
        ];

        if (gui_store.feature_active("vm")) {
            children.push(run_button, resume_button, step_over_button, step_in_button, step_out_button);
        }

        super({ children });

        const quest_loaded = quest_editor_store.current_quest.map(q => q != undefined);

        this.disposables(
            new_quest_button.chosen.observe(({ value: episode }) =>
                quest_editor_store.new_quest(episode),
            ),

            open_file_button.files.observe(({ value: files }) => {
                if (files.length) {
                    quest_editor_store.open_file(files[0]);
                }
            }),

            save_as_button.enabled.bind_to(quest_loaded),
            save_as_button.click.observe(quest_editor_store.save_as),

            undo_button.enabled.bind_to(undo_manager.can_undo),
            undo_button.click.observe(() => undo_manager.undo()),

            redo_button.enabled.bind_to(undo_manager.can_redo),
            redo_button.click.observe(() => undo_manager.redo()),

            area_select.enabled.bind_to(quest_loaded),
            area_select.selected.bind_to(quest_editor_store.current_area),
            area_select.selected.observe(({ value: area }) =>
                quest_editor_store.set_current_area(area),
            ),

            run_button.click.observe(quest_editor_store.run_current_quest),

            gui_store.on_global_keydown(GuiTool.QuestEditor, "Ctrl-O", () =>
                open_file_button.click(),
            ),

            gui_store.on_global_keydown(
                GuiTool.QuestEditor,
                "Ctrl-Shift-S",
                quest_editor_store.save_as,
            ),

            gui_store.on_global_keydown(GuiTool.QuestEditor, "Ctrl-Z", () => {
                undo_manager.undo();
            }),

            gui_store.on_global_keydown(GuiTool.QuestEditor, "Ctrl-Shift-Z", () => {
                undo_manager.redo();
            }),

            gui_store.on_global_keydown(
                GuiTool.QuestEditor,
                "F5",
                quest_editor_store.run_current_quest,
            ),
        );

        this.finalize_construction(QuestEditorToolBar.prototype);
    }
}
