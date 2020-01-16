import { ToolBar } from "../../core/gui/ToolBar";
import { FileButton } from "../../core/gui/FileButton";
import { Button } from "../../core/gui/Button";
import { undo_manager } from "../../core/undo/UndoManager";
import { Select } from "../../core/gui/Select";
import { div, Icon } from "../../core/gui/dom";
import { DropDown } from "../../core/gui/DropDown";
import { Episode } from "../../core/data_formats/parsing/quest/Episode";
import {
    AreaAndLabel,
    QuestEditorToolBarController,
} from "../controllers/QuestEditorToolBarController";
import { View } from "../../core/gui/View";
import { Dialog } from "../../core/gui/Dialog";
import { TextInput } from "../../core/gui/TextInput";
import "./QuestEditorToolBarView.css";
import { Version } from "../../core/data_formats/parsing/quest/Version";

export class QuestEditorToolBarView extends View {
    private readonly toolbar: ToolBar;

    get element(): HTMLElement {
        return this.toolbar.element;
    }

    get height(): number {
        return this.toolbar.height;
    }

    constructor(ctrl: QuestEditorToolBarController) {
        super();

        const new_quest_button = new DropDown({
            text: "New quest",
            icon_left: Icon.NewFile,
            items: [Episode.I],
            to_label: episode => `Episode ${Episode[episode]}`,
        });
        const open_file_button = new FileButton({
            icon_left: Icon.File,
            text: "Open file...",
            accept: ".bin, .dat, .qst",
            multiple: true,
            tooltip: "Open a quest file (Ctrl-O)",
        });
        const save_as_button = new Button({
            text: "Save as...",
            icon_left: Icon.Save,
            tooltip: "Save this quest to new file (Ctrl-Shift-S)",
        });
        const undo_button = new Button({
            text: "Undo",
            icon_left: Icon.Undo,
            tooltip: undo_manager.first_undo.map(
                action =>
                    (action ? `Undo "${action.description}"` : "Nothing to undo") + " (Ctrl-Z)",
            ),
        });
        const redo_button = new Button({
            text: "Redo",
            icon_left: Icon.Redo,
            tooltip: undo_manager.first_redo.map(
                action =>
                    (action ? `Redo "${action.description}"` : "Nothing to redo") +
                    " (Ctrl-Shift-Z)",
            ),
        });
        // TODO: make sure select menu is updated when entity counts change.
        const area_select = new Select<AreaAndLabel>({
            items: ctrl.areas,
            to_label: ({ label }) => label,
        });
        const debug_button = new Button({
            text: "Debug",
            icon_left: Icon.Play,
            tooltip: "Debug the current quest in a virtual machine (F5)",
        });
        const resume_button = new Button({
            text: "Continue",
            icon_left: Icon.SquareArrowRight,
            tooltip: "Resume execution (F6)",
        });
        const step_over_button = new Button({
            text: "Step over",
            icon_left: Icon.LongArrowRight,
            tooltip: "Execute the next line and step over any function calls (F8)",
        });
        const step_in_button = new Button({
            text: "Step into",
            icon_left: Icon.LevelDown,
            tooltip: "Execute the next line and step inside any function calls (F7)",
        });
        const step_out_button = new Button({
            text: "Step out",
            icon_left: Icon.LevelUp,
            tooltip: "Execute until outside of current call frame (Shift-F8)",
        });
        const stop_button = new Button({
            text: "Stop",
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

        if (ctrl.vm_feature_active) {
            children.push(
                debug_button,
                resume_button,
                step_over_button,
                step_in_button,
                step_out_button,
                stop_button,
            );
        }

        this.toolbar = this.disposable(new ToolBar(...children));

        // "Save As" dialog.
        const filename_input = this.disposable(
            new TextInput(ctrl.filename.val, { label: "File name:" }),
        );
        const version_select = this.disposable(
            new Select({
                label: "Version:",
                items: [Version.GC, Version.BB],
                selected: ctrl.version,
                to_label: version => {
                    switch (version) {
                        case Version.DC:
                            return "Dreamcast";
                        case Version.GC:
                            return "GameCube";
                        case Version.PC:
                            return "PC";
                        case Version.BB:
                            return "BlueBurst";
                    }
                },
            }),
        );
        const save_button = this.disposable(new Button({ text: "Save" }));
        const cancel_button = this.disposable(new Button({ text: "Cancel" }));

        const save_as_dialog = this.disposable(
            new Dialog({
                title: "Save As",
                visible: ctrl.save_as_dialog_visible,
                content: div(
                    { className: "quest_editor_QuestEditorToolBarView_save_as_dialog_content" },
                    filename_input.label!.element,
                    filename_input.element,
                    version_select.label!.element,
                    version_select.element,
                ),
                footer: [save_button.element, cancel_button.element],
            }),
        );

        save_as_dialog.element.addEventListener("keydown", evt => {
            if (evt.key === "Enter") {
                ctrl.save_as();
            }
        });

        this.disposables(
            new_quest_button.chosen.observe(({ value: episode }) => ctrl.create_new_quest(episode)),

            open_file_button.files.observe(({ value: files }) => ctrl.parse_files(files)),

            save_as_button.onclick.observe(ctrl.save_as_clicked),
            save_as_button.enabled.bind_to(ctrl.can_save),

            save_as_dialog.ondismiss.observe(ctrl.dismiss_save_as_dialog),

            filename_input.value.bind_to(ctrl.filename),
            filename_input.value.observe(({ value }) => ctrl.set_filename(value)),

            version_select.selected.observe(({ value }) => {
                if (value != undefined) {
                    ctrl.set_version(value);
                }
            }),

            save_button.onclick.observe(ctrl.save_as),
            cancel_button.onclick.observe(ctrl.dismiss_save_as_dialog),

            undo_button.onclick.observe(() => undo_manager.undo()),
            undo_button.enabled.bind_to(ctrl.can_undo),

            redo_button.onclick.observe(() => undo_manager.redo()),
            redo_button.enabled.bind_to(ctrl.can_redo),

            area_select.selected.bind_to(ctrl.current_area),
            area_select.selected.observe(({ value }) => ctrl.set_area(value!)),
            area_select.enabled.bind_to(ctrl.can_select_area),

            debug_button.onclick.observe(ctrl.debug),
            debug_button.enabled.bind_to(ctrl.can_debug),

            resume_button.onclick.observe(ctrl.resume),
            resume_button.enabled.bind_to(ctrl.can_step),

            step_over_button.onclick.observe(ctrl.step_over),
            step_over_button.enabled.bind_to(ctrl.can_step),

            step_in_button.onclick.observe(ctrl.step_in),
            step_in_button.enabled.bind_to(ctrl.can_step),

            step_out_button.onclick.observe(ctrl.step_out),
            step_out_button.enabled.bind_to(ctrl.can_step),

            stop_button.onclick.observe(ctrl.stop),
            stop_button.enabled.bind_to(ctrl.can_stop),
        );

        this.finalize_construction();
    }
}
