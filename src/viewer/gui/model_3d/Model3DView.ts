import "./Model3DView.css";
import { RendererWidget } from "../../../core/gui/RendererWidget";
import { Model3DRenderer } from "../../rendering/Model3DRenderer";
import { Model3DToolBarView } from "./Model3DToolBarView";
import { Model3DSelectListView } from "./Model3DSelectListView";
import { CharacterClassModel } from "../../model/CharacterClassModel";
import { CharacterClassAnimationModel } from "../../model/CharacterClassAnimationModel";
import { Model3DStore } from "../../stores/Model3DStore";
import { DisposableThreeRenderer } from "../../../core/rendering/Renderer";
import { div } from "../../../core/gui/dom";
import { ResizableView } from "../../../core/gui/ResizableView";
import { GuiStore } from "../../../core/stores/GuiStore";

const MODEL_LIST_WIDTH = 100;
const ANIMATION_LIST_WIDTH = 140;

export class Model3DView extends ResizableView {
    readonly element = div({ className: "viewer_Model3DView" });

    private tool_bar_view: Model3DToolBarView;
    private model_list_view: Model3DSelectListView<CharacterClassModel>;
    private animation_list_view: Model3DSelectListView<CharacterClassAnimationModel>;
    private renderer_view: RendererWidget;

    constructor(
        private readonly gui_store: GuiStore,
        model_3d_store: Model3DStore,
        three_renderer: DisposableThreeRenderer,
    ) {
        super();

        this.tool_bar_view = this.add(new Model3DToolBarView(model_3d_store));
        this.model_list_view = this.add(
            new Model3DSelectListView(
                model_3d_store.models,
                model_3d_store.current_model,
                model_3d_store.set_current_model,
            ),
        );
        this.animation_list_view = this.add(
            new Model3DSelectListView(
                model_3d_store.animations,
                model_3d_store.current_animation,
                model_3d_store.set_current_animation,
            ),
        );
        this.renderer_view = this.add(
            new RendererWidget(new Model3DRenderer(three_renderer, model_3d_store)),
        );

        this.animation_list_view.borders = true;

        this.element.append(
            this.tool_bar_view.element,
            div(
                { className: "viewer_Model3DView_container" },
                this.model_list_view.element,
                this.animation_list_view.element,
                this.renderer_view.element,
            ),
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
