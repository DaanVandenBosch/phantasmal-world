import { ToolBar } from "../../core/gui/ToolBar";
import { CheckBox } from "../../core/gui/CheckBox";
import { asm_editor_store } from "../stores/AsmEditorStore";

export class AsmEditorToolBar extends ToolBar {
    constructor() {
        const inline_args_mode_checkbox = new CheckBox(true, {
            label: "Inline args mode",
            tooltip: asm_editor_store.has_issues.map((has_issues) => {
                let text = "Transform arg_push* opcodes to be inline with the opcode the arguments are given to.";

                if (has_issues) {
                    text += "\nThis mode cannot be toggled because there are issues in the script.";
                }

                return text;
            }),
        });

        super({
            children: [
                inline_args_mode_checkbox
            ],
        });

        this.disposables(
            asm_editor_store.inline_args_mode.bind_to(inline_args_mode_checkbox.checked),

            inline_args_mode_checkbox.enabled.bind_to(asm_editor_store.has_issues.map(b => !b)),
        );


        this.finalize_construction(AsmEditorToolBar.prototype);
    }
}
