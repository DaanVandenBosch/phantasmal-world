import "./ModelView.css";
import { RendererWidget } from "../../../core/gui/RendererWidget";
import { ModelToolBarView } from "./ModelToolBarView";
import { CharacterClassSelectionView } from "./CharacterClassSelectionView";
import { CharacterClassModel } from "../../model/CharacterClassModel";
import { CharacterClassAnimationModel } from "../../model/CharacterClassAnimationModel";
import { ModelController } from "../../controllers/model/ModelController";
import { div } from "../../../core/gui/dom";
import { ResizableView } from "../../../core/gui/ResizableView";
import { CharacterClassOptionsView } from "./CharacterClassOptionsView";
import { Renderer } from "../../../core/rendering/Renderer";

const CHARACTER_CLASS_SELECTION_WIDTH = 100;
const CHARACTER_CLASS_OPTIONS_WIDTH = 220;
const ANIMATION_SELECTION_WIDTH = 140;

export class ModelView extends ResizableView {
    readonly element = div({ className: "viewer_model_ModelView" });

    private tool_bar_view: ModelToolBarView;
    private character_class_selection_view: CharacterClassSelectionView<CharacterClassModel>;
    private options_view: CharacterClassOptionsView;
    private renderer_view: RendererWidget;
    private animation_selection_view: CharacterClassSelectionView<CharacterClassAnimationModel>;

    constructor(
        ctrl: ModelController,
        tool_bar_view: ModelToolBarView,
        options_view: CharacterClassOptionsView,
        renderer: Renderer,
    ) {
        super();

        this.tool_bar_view = this.add(tool_bar_view);
        this.character_class_selection_view = this.add(
            new CharacterClassSelectionView(
                ctrl.character_classes,
                ctrl.current_character_class,
                ctrl.set_current_character_class,
                false,
            ),
        );
        this.options_view = this.add(options_view);
        this.renderer_view = this.add(new RendererWidget(renderer));
        this.animation_selection_view = this.add(
            new CharacterClassSelectionView(
                ctrl.animations,
                ctrl.current_animation,
                ctrl.set_current_animation,
                true,
            ),
        );

        this.element.append(
            this.tool_bar_view.element,
            div(
                { className: "viewer_model_ModelView_container" },
                this.character_class_selection_view.element,
                this.options_view.element,
                this.renderer_view.element,
                this.animation_selection_view.element,
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

        this.character_class_selection_view.resize(
            CHARACTER_CLASS_SELECTION_WIDTH,
            container_height,
        );
        this.options_view.resize(CHARACTER_CLASS_OPTIONS_WIDTH, container_height);
        this.renderer_view.resize(
            Math.max(
                0,
                width -
                    CHARACTER_CLASS_SELECTION_WIDTH -
                    CHARACTER_CLASS_OPTIONS_WIDTH -
                    ANIMATION_SELECTION_WIDTH,
            ),
            container_height,
        );
        this.animation_selection_view.resize(ANIMATION_SELECTION_WIDTH, container_height);

        return this;
    }
}
