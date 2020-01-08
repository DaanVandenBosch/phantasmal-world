import { div, Icon } from "../../core/gui/dom";
import { FileButton } from "../../core/gui/FileButton";
import { ToolBar } from "../../core/gui/ToolBar";
import { RendererWidget } from "../../core/gui/RendererWidget";
import { TextureRenderer } from "../rendering/TextureRenderer";
import { ResizableView } from "../../core/gui/ResizableView";
import { TextureController } from "../controllers/TextureController";
import { Dialog, show_result_in_dialog } from "../../core/gui/Dialog";

export class TextureView extends ResizableView {
    readonly element = div({ className: "viewer_TextureView" });

    private readonly open_file_button = new FileButton({
        icon_left: Icon.File,
        text: "Open file...",
        accept: ".afs, .xvm",
    });

    private readonly tool_bar = this.add(new ToolBar(this.open_file_button));

    private readonly renderer_view: RendererWidget;

    constructor(ctrl: TextureController, renderer: TextureRenderer) {
        super();

        this.renderer_view = this.add(new RendererWidget(renderer));

        this.element.append(this.tool_bar.element, this.renderer_view.element);

        const dialog = this.disposable(new Dialog());

        this.disposables(
            this.open_file_button.files.observe(async ({ value: files }) => {
                if (files.length) {
                    const file = files[0];
                    const result = await ctrl.load_file(file);

                    show_result_in_dialog(
                        dialog,
                        result,
                        `Encountered some problems while opening "${file.name}".`,
                        `Couldn't open "${file.name}".`,
                    );
                }
            }),
        );

        this.finalize_construction();
    }

    activate(): void {
        this.renderer_view.start_rendering();
        super.activate();
    }

    deactivate(): void {
        super.deactivate();
        this.renderer_view.stop_rendering();
    }

    resize(width: number, height: number): this {
        super.resize(width, height);

        this.renderer_view.resize(width, Math.max(0, height - this.tool_bar.height));

        return this;
    }
}
