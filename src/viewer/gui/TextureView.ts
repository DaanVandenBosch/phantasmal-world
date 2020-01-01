import { div, Icon } from "../../core/gui/dom";
import { ResizableWidget } from "../../core/gui/ResizableWidget";
import { FileButton } from "../../core/gui/FileButton";
import { ToolBar } from "../../core/gui/ToolBar";
import { RendererWidget } from "../../core/gui/RendererWidget";
import { TextureRenderer } from "../rendering/TextureRenderer";
import { GuiStore, GuiTool } from "../../core/stores/GuiStore";
import { TextureStore } from "../stores/TextureStore";
import { DisposableThreeRenderer } from "../../core/rendering/Renderer";

export class TextureView extends ResizableWidget {
    readonly element = div({ className: "viewer_TextureView" });

    private readonly open_file_button = new FileButton("Open file...", {
        icon_left: Icon.File,
        accept: ".afs, .xvm",
    });

    private readonly tool_bar = this.disposable(new ToolBar(this.open_file_button));

    private readonly renderer_view: RendererWidget;

    constructor(
        gui_store: GuiStore,
        texture_store: TextureStore,
        three_renderer: DisposableThreeRenderer,
    ) {
        super();

        this.renderer_view = this.disposable(
            new RendererWidget(new TextureRenderer(three_renderer, texture_store)),
        );

        this.element.append(this.tool_bar.element, this.renderer_view.element);

        this.disposable(
            this.open_file_button.files.observe(({ value: files }) => {
                if (files.length) texture_store.load_file(files[0]);
            }),
        );

        this.renderer_view.start_rendering();

        this.disposables(
            gui_store.tool.observe(({ value: tool }) => {
                if (tool === GuiTool.Viewer) {
                    this.renderer_view.start_rendering();
                } else {
                    this.renderer_view.stop_rendering();
                }
            }),
        );

        this.finalize_construction();
    }

    resize(width: number, height: number): this {
        super.resize(width, height);

        this.renderer_view.resize(width, Math.max(0, height - this.tool_bar.height));

        return this;
    }
}
