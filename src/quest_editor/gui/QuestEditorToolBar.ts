import { ToolBar } from "../../core/gui/ToolBar";
import { FileButton } from "../../core/gui/FileButton";
import { Button } from "../../core/gui/Button";
import { undo_manager } from "../../core/undo/UndoManager";
import { Select } from "../../core/gui/Select";
import { list_property, map } from "../../core/observable";
import { AreaModel } from "../model/AreaModel";
import { Icon } from "../../core/gui/dom";
import { DropDown } from "../../core/gui/DropDown";
import { Episode } from "../../core/data_formats/parsing/quest/Episode";
import { GuiStore, GuiTool } from "../../core/stores/GuiStore";
import { QuestEditorStore } from "../stores/QuestEditorStore";
import { AreaStore } from "../stores/AreaStore";

export class QuestEditorToolBar extends ToolBar {
    constructor(gui_store: GuiStore, area_store: AreaStore, quest_editor_store: QuestEditorStore) {
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
        const debug_button = new Button("Debug", {
            icon_left: Icon.Play,
            tooltip: "Debug the current quest in a virtual machine (F5)",
        });
        const resume_button = new Button("Continue", {
            icon_left: Icon.SquareArrowRight,
            tooltip: "Resume execution (F6)",
        });
        const step_over_button = new Button("Step over", {
            icon_left: Icon.LongArrowRight,
            tooltip: "Execute the next line and step over any function calls (F8)",
        });
        const step_in_button = new Button("Step into", {
            icon_left: Icon.LevelDown,
            tooltip: "Execute the next line and step inside any function calls (F7)",
        });
        const step_out_button = new Button("Step out", {
            icon_left: Icon.LevelUp,
            tooltip: "Execute until outside of current call frame (Shift-F8)",
        });
        const stop_button = new Button("Stop", {
            icon_left: Icon.Stop,
            tooltip: "Stop execution (Shift-F5)",
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
            children.push(
                debug_button,
                resume_button,
                step_over_button,
                step_in_button,
                step_out_button,
                stop_button,
            );
        }

        super({ children });

        const quest_loaded = quest_editor_store.current_quest.map(q => q != undefined);

        const step_controls_enabled = quest_editor_store.quest_runner.paused;

        this.disposables(
            new_quest_button.chosen.observe(({ value: episode }) =>
                quest_editor_store.new_quest(episode),
            ),

            open_file_button.files.observe(({ value: files }) => {
                if (files.length) {
                    quest_editor_store.open_file(files[0]);
                }
            }),

            save_as_button.click.observe(quest_editor_store.save_as),
            save_as_button.enabled.bind_to(quest_loaded),

            undo_button.click.observe(() => undo_manager.undo()),
            undo_button.enabled.bind_to(
                map(
                    (c, r) => c && !r,
                    undo_manager.can_undo,
                    quest_editor_store.quest_runner.running,
                ),
            ),

            redo_button.click.observe(() => undo_manager.redo()),
            redo_button.enabled.bind_to(
                map(
                    (c, r) => c && !r,
                    undo_manager.can_redo,
                    quest_editor_store.quest_runner.running,
                ),
            ),

            area_select.selected.bind_to(quest_editor_store.current_area),
            area_select.selected.observe(({ value: area }) =>
                quest_editor_store.set_current_area(area),
            ),
            area_select.enabled.bind_to(quest_loaded),

            debug_button.click.observe(quest_editor_store.debug_current_quest),
            debug_button.enabled.bind_to(quest_loaded),

            resume_button.click.observe(() => quest_editor_store.quest_runner.resume()),
            resume_button.enabled.bind_to(step_controls_enabled),

            step_over_button.click.observe(() => quest_editor_store.quest_runner.step_over()),
            step_over_button.enabled.bind_to(step_controls_enabled),

            step_in_button.click.observe(() => quest_editor_store.quest_runner.step_into()),
            step_in_button.enabled.bind_to(step_controls_enabled),

            step_out_button.click.observe(() => quest_editor_store.quest_runner.step_out()),
            step_out_button.enabled.bind_to(step_controls_enabled),

            stop_button.click.observe(() => quest_editor_store.quest_runner.stop()),
            stop_button.enabled.bind_to(quest_editor_store.quest_runner.running),

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
                quest_editor_store.debug_current_quest,
            ),

            gui_store.on_global_keydown(GuiTool.QuestEditor, "Shift-F5", () =>
                quest_editor_store.quest_runner.stop(),
            ),

            gui_store.on_global_keydown(GuiTool.QuestEditor, "F6", () =>
                quest_editor_store.quest_runner.resume(),
            ),

            gui_store.on_global_keydown(GuiTool.QuestEditor, "F8", () =>
                quest_editor_store.quest_runner.step_over(),
            ),

            gui_store.on_global_keydown(GuiTool.QuestEditor, "F7", () =>
                quest_editor_store.quest_runner.step_into(),
            ),

            gui_store.on_global_keydown(GuiTool.QuestEditor, "Shift-F8", () =>
                quest_editor_store.quest_runner.step_out(),
            ),
        );

        this.finalize_construction();
    }
}
