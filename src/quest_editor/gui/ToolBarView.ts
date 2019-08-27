import { Widget } from "../../core/gui/Widget";
import { ToolBar } from "../../core/gui/ToolBar";
import { FileButton } from "../../core/gui/FileButton";
import { Button } from "../../core/gui/Button";
import { quest_editor_store } from "../stores/QuestEditorStore";
import { undo_manager } from "../../core/undo/UndoManager";

export class ToolBarView extends Widget {
    private readonly open_file_button = new FileButton("Open file...", ".qst");
    private readonly save_as_button = new Button("Save as...");
    private readonly undo_button = new Button("Undo");
    private readonly redo_button = new Button("Redo");

    private readonly tool_bar = new ToolBar(
        this.open_file_button,
        this.save_as_button,
        this.undo_button,
        this.redo_button,
    );

    readonly element = this.tool_bar.element;

    get height(): number {
        return this.tool_bar.height;
    }

    constructor() {
        super();

        this.disposables(
            this.open_file_button.files.observe(({ value: files }) => {
                if (files.length) {
                    quest_editor_store.open_file(files[0]);
                }
            }),

            this.save_as_button.enabled.bind_to(
                quest_editor_store.current_quest.map(q => q != undefined),
            ),

            this.undo_button.enabled.bind_to(undo_manager.can_undo),
            this.undo_button.click.observe(() => undo_manager.undo()),

            this.redo_button.enabled.bind_to(undo_manager.can_redo),
            this.redo_button.click.observe(() => undo_manager.redo()),
        );
    }
}
