import { ToolBar } from "../../core/gui/ToolBar";
import { Button } from "../../core/gui/Button";

export class AsmEditorToolBar extends ToolBar {
    constructor() {

        super({
            children: [],
        });
        this.finalize_construction(AsmEditorToolBar.prototype);
    }
}
