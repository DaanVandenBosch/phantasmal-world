import { ToolBar } from "../../core/gui/ToolBar";
import { FileButton } from "../../core/gui/FileButton";
import { Button } from "../../core/gui/Button";
import { quest_editor_store } from "../stores/QuestEditorStore";
import { undo_manager } from "../../core/undo/UndoManager";

export class QuestEditorToolBar extends ToolBar {
    constructor() {
        const open_file_button = new FileButton("Open file...", ".qst");
        const save_as_button = new Button("Save as...");
        const undo_button = new Button("Undo", {
            tooltip: undo_manager.first_undo.map(action =>
                action ? `Undo "${action.description}"` : "Nothing to undo",
            ),
        });
        const redo_button = new Button("Redo", {
            tooltip: undo_manager.first_redo.map(action =>
                action ? `Redo "${action.description}"` : "Nothing to redo",
            ),
        });

        super({
            children: [open_file_button, save_as_button, undo_button, redo_button],
        });

        this.disposables(
            open_file_button.files.observe(({ value: files }) => {
                if (files.length) {
                    quest_editor_store.open_file(files[0]);
                }
            }),

            save_as_button.enabled.bind_to(
                quest_editor_store.current_quest.map(q => q != undefined),
            ),

            undo_button.enabled.bind_to(undo_manager.can_undo),
            undo_button.click.observe(() => undo_manager.undo()),

            redo_button.enabled.bind_to(undo_manager.can_redo),
            redo_button.click.observe(() => undo_manager.redo()),
        );
    }
}
