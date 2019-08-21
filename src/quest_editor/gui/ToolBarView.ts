import { View } from "../../core/gui/View";
import { ToolBar } from "../../core/gui/ToolBar";
import { FileButton } from "../../core/gui/FileButton";
import { Button } from "../../core/gui/Button";

export class ToolBarView extends View {
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
}
