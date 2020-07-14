import { div, Icon } from "../../../core/gui/dom";
import { FileButton } from "../../../core/gui/FileButton";
import { ToolBar } from "../../../core/gui/ToolBar";
import { RendererWidget } from "../../../core/gui/RendererWidget";
import { ResizableView } from "../../../core/gui/ResizableView";
import { TextureController } from "../../controllers/texture/TextureController";
import { ResultDialog } from "../../../core/gui/ResultDialog";
import { Renderer } from "../../../core/rendering/Renderer";

export class TextureView extends ResizableView {
    readonly element = div({ className: "viewer_TextureView" });

    private readonly open_file_button = new FileButton({
        icon_left: Icon.File,
        text: "Open file...",
        accept: ".afs, .xvm",
    });

    private readonly tool_bar = this.add(new ToolBar(this.open_file_button));

    private readonly renderer_widget: RendererWidget;

    constructor(ctrl: TextureController, renderer: Renderer) {
        super();

        this.renderer_widget = this.add(new RendererWidget(renderer));

        this.element.append(this.tool_bar.element, this.renderer_widget.element);

        const dialog = this.disposable(
            new ResultDialog({
                visible: ctrl.result_dialog_visible,
                result: ctrl.result,
                problems_message: ctrl.result_problems_message,
                error_message: ctrl.result_error_message,
            }),
        );

        this.disposables(
            this.open_file_button.files.observe(({ value: files }) => {
                if (files.length) {
                    ctrl.load_file(files[0]);
                }
            }),

            dialog.ondismiss.observe(ctrl.dismiss_result_dialog),
        );

        this.finalize_construction(TextureView);
    }

    resize(width: number, height: number): this {
        super.resize(width, height);

        this.renderer_widget.resize(width, Math.max(0, height - this.tool_bar.height));

        return this;
    }
}
