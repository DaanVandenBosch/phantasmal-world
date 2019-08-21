import { create_el } from "../../core/gui/dom";
import { ResizableView } from "../../core/gui/ResizableView";
import { FileButton } from "../../core/gui/FileButton";
import { ToolBar } from "../../core/gui/ToolBar";
import { texture_store } from "../stores/TextureStore";
import { RendererView } from "../../core/gui/RendererView";
import { TextureRenderer } from "../rendering/TextureRenderer";
import { gui_store, GuiTool } from "../../core/stores/GuiStore";

export class TextureView extends ResizableView {
    readonly element = create_el("div", "viewer_TextureView");

    private readonly open_file_button = new FileButton("Open file...", ".xvm");

    private readonly tool_bar = this.disposable(new ToolBar(this.open_file_button));

    private readonly renderer_view = this.disposable(new RendererView(new TextureRenderer()));

    constructor() {
        super();

        this.element.append(this.tool_bar.element, this.renderer_view.element);

        this.disposable(
            this.open_file_button.files.observe(files => {
                if (files.length) texture_store.load_file(files[0]);
            }),
        );

        this.renderer_view.start_rendering();

        this.disposable(
            gui_store.tool.observe(tool => {
                if (tool === GuiTool.Viewer) {
                    this.renderer_view.start_rendering();
                } else {
                    this.renderer_view.stop_rendering();
                }
            }),
        );
    }

    resize(width: number, height: number): this {
        super.resize(width, height);

        this.renderer_view.resize(width, Math.max(0, height - this.tool_bar.height));

        return this;
    }
}
