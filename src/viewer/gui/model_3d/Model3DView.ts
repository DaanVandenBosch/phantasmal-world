import { el } from "../../../core/gui/dom";
import { ResizableWidget } from "../../../core/gui/ResizableWidget";
import "./Model3DView.css";
import { gui_store, GuiTool } from "../../../core/stores/GuiStore";
import { RendererWidget } from "../../../core/gui/RendererWidget";
import { model_store } from "../../stores/Model3DStore";
import { Model3DRenderer } from "../../rendering/Model3DRenderer";
import { Model3DToolBar } from "./Model3DToolBar";
import { Model3DSelectListView } from "./Model3DSelectListView";
import { CharacterClassModel } from "../../model/CharacterClassModel";
import { CharacterClassAnimationModel } from "../../model/CharacterClassAnimationModel";

const MODEL_LIST_WIDTH = 100;
const ANIMATION_LIST_WIDTH = 140;

export class Model3DView extends ResizableWidget {
    readonly element = el.div({ class: "viewer_Model3DView" });

    private tool_bar_view: Model3DToolBar;
    private model_list_view: Model3DSelectListView<CharacterClassModel>;
    private animation_list_view: Model3DSelectListView<CharacterClassAnimationModel>;
    private renderer_view: RendererWidget;

    constructor() {
        super();

        this.tool_bar_view = this.disposable(new Model3DToolBar());
        this.model_list_view = this.disposable(
            new Model3DSelectListView(
                model_store.models,
                model_store.current_model,
                model_store.set_current_model,
            ),
        );
        this.animation_list_view = this.disposable(
            new Model3DSelectListView(
                model_store.animations,
                model_store.current_animation,
                model_store.set_current_animation,
            ),
        );
        this.renderer_view = this.disposable(new RendererWidget(new Model3DRenderer()));

        this.animation_list_view.borders = true;

        this.element.append(
            this.tool_bar_view.element,
            el.div(
                { class: "viewer_Model3DView_container" },
                this.model_list_view.element,
                this.animation_list_view.element,
                this.renderer_view.element,
            ),
        );

        model_store.set_current_model(model_store.models[5]);

        this.renderer_view.start_rendering();

        this.disposable(
            gui_store.tool.observe(({ value: tool }) => {
                if (tool === GuiTool.Viewer) {
                    this.renderer_view.start_rendering();
                } else {
                    this.renderer_view.stop_rendering();
                }
            }),
        );

        this.finalize_construction(Model3DView.prototype);
    }

    resize(width: number, height: number): this {
        super.resize(width, height);

        const container_height = Math.max(0, height - this.tool_bar_view.height);

        this.model_list_view.resize(MODEL_LIST_WIDTH, container_height);
        this.animation_list_view.resize(ANIMATION_LIST_WIDTH, container_height);
        this.renderer_view.resize(
            Math.max(0, width - MODEL_LIST_WIDTH - ANIMATION_LIST_WIDTH),
            container_height,
        );

        return this;
    }
}
