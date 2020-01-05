import { div, Icon } from "../../core/gui/dom";
import { FileButton } from "../../core/gui/FileButton";
import { ToolBar } from "../../core/gui/ToolBar";
import { RendererWidget } from "../../core/gui/RendererWidget";
import { TextureRenderer } from "../rendering/TextureRenderer";
import { TextureStore } from "../stores/TextureStore";
import { DisposableThreeRenderer } from "../../core/rendering/Renderer";
import { ResizableView } from "../../core/gui/ResizableView";

export class TextureView extends ResizableView {
    readonly element = div({ className: "viewer_TextureView" });

    private readonly open_file_button = new FileButton("Open file...", {
        icon_left: Icon.File,
        accept: ".afs, .xvm",
    });

    private readonly tool_bar = this.add(new ToolBar(this.open_file_button));

    private readonly renderer_view: RendererWidget;

    constructor(texture_store: TextureStore, three_renderer: DisposableThreeRenderer) {
        super();

        this.renderer_view = this.add(
            new RendererWidget(new TextureRenderer(three_renderer, texture_store)),
        );

        this.element.append(this.tool_bar.element, this.renderer_view.element);

        this.disposables(
            this.open_file_button.files.observe(({ value: files }) => {
                if (files.length) texture_store.load_file(files[0]);
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
