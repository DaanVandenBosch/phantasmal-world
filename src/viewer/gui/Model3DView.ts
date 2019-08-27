import { create_element } from "../../core/gui/dom";
import { ResizableWidget } from "../../core/gui/ResizableWidget";
import { ToolBar } from "../../core/gui/ToolBar";
import "./Model3DView.css";
import { model_store } from "../stores/Model3DStore";
import { WritableProperty } from "../../core/observable/WritableProperty";
import { RendererWidget } from "../../core/gui/RendererWidget";
import { Model3DRenderer } from "../rendering/Model3DRenderer";
import { Widget } from "../../core/gui/Widget";
import { FileButton } from "../../core/gui/FileButton";
import { CheckBox } from "../../core/gui/CheckBox";
import { NumberInput } from "../../core/gui/NumberInput";
import { Label } from "../../core/gui/Label";
import { gui_store, GuiTool } from "../../core/stores/GuiStore";
import { PSO_FRAME_RATE } from "../../core/rendering/conversion/ninja_animation";

const MODEL_LIST_WIDTH = 100;
const ANIMATION_LIST_WIDTH = 140;

export class Model3DView extends ResizableWidget {
    readonly element = create_element("div", { class: "viewer_Model3DView" });

    private tool_bar_view = this.disposable(new ToolBarView());
    private container_element = create_element("div", { class: "viewer_Model3DView_container" });
    private model_list_view = this.disposable(
        new ModelSelectListView(model_store.models, model_store.current_model),
    );
    private animation_list_view = this.disposable(
        new ModelSelectListView(model_store.animations, model_store.current_animation),
    );
    private renderer_view = this.disposable(new RendererWidget(new Model3DRenderer()));

    constructor() {
        super();

        this.animation_list_view.borders = true;

        this.container_element.append(
            this.model_list_view.element,
            this.animation_list_view.element,
            this.renderer_view.element,
        );

        this.element.append(this.tool_bar_view.element, this.container_element);

        model_store.current_model.val = model_store.models[5];

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

class ToolBarView extends Widget {
    private readonly open_file_button = new FileButton("Open file...", ".nj, .njm, .xj, .xvm");
    private readonly skeleton_checkbox = new CheckBox(false, { label: "Show skeleton" });
    private readonly play_animation_checkbox = new CheckBox(true, { label: "Play animation" });
    private readonly animation_frame_rate_input = new NumberInput(PSO_FRAME_RATE, {
        label: "Frame rate:",
        min: 1,
        max: 240,
        step: 1,
    });
    private readonly animation_frame_input = new NumberInput(1, {
        label: "Frame:",
        min: 1,
        max: model_store.animation_frame_count,
        step: 1,
    });
    private readonly animation_frame_count_label = new Label(
        model_store.animation_frame_count.map(count => `/ ${count}`),
    );

    private readonly tool_bar = this.disposable(
        new ToolBar(
            this.open_file_button,
            this.skeleton_checkbox,
            this.play_animation_checkbox,
            this.animation_frame_rate_input,
            this.animation_frame_input,
            this.animation_frame_count_label,
        ),
    );

    readonly element = this.tool_bar.element;

    get height(): number {
        return this.tool_bar.height;
    }

    constructor() {
        super();

        // Always-enabled controls.
        this.disposables(
            this.open_file_button.files.observe(({ value: files }) => {
                if (files.length) model_store.load_file(files[0]);
            }),

            model_store.show_skeleton.bind_to(this.skeleton_checkbox.checked),
        );

        // Controls that are only enabled when an animation is selected.
        const enabled = model_store.current_nj_motion.map(njm => njm != undefined);

        this.disposables(
            this.play_animation_checkbox.enabled.bind_to(enabled),
            model_store.animation_playing.bind_bi(this.play_animation_checkbox.checked),

            this.animation_frame_rate_input.enabled.bind_to(enabled),
            model_store.animation_frame_rate.bind_to(this.animation_frame_rate_input.value),

            this.animation_frame_input.enabled.bind_to(enabled),
            model_store.animation_frame.bind_to(this.animation_frame_input.value),
            this.animation_frame_input.value.bind_to(
                model_store.animation_frame.map(v => Math.round(v)),
            ),

            this.animation_frame_count_label.enabled.bind_to(enabled),
        );
    }
}

class ModelSelectListView<T extends { name: string }> extends ResizableWidget {
    element = create_element("ul", { class: "viewer_ModelSelectListView" });

    set borders(borders: boolean) {
        if (borders) {
            this.element.style.borderLeft = "solid 1px var(--border-color)";
            this.element.style.borderRight = "solid 1px var(--border-color)";
        } else {
            this.element.style.borderLeft = "none";
            this.element.style.borderRight = "none";
        }
    }

    private selected_model?: T;
    private selected_element?: HTMLLIElement;

    constructor(private models: T[], private selected: WritableProperty<T | undefined>) {
        super();

        this.element.onclick = this.list_click;

        models.forEach((model, index) => {
            this.element.append(
                create_element("li", { text: model.name, data: { index: index.toString() } }),
            );
        });

        this.disposable(
            selected.observe(({ value: model }) => {
                if (this.selected_element) {
                    this.selected_element.classList.remove("active");
                    this.selected_element = undefined;
                }

                if (model && model !== this.selected_model) {
                    const index = this.models.indexOf(model);

                    if (index !== -1) {
                        this.selected_element = this.element.childNodes[index] as HTMLLIElement;
                        this.selected_element.classList.add("active");
                    }
                }
            }),
        );
    }

    private list_click = (e: MouseEvent) => {
        if (e.target instanceof HTMLLIElement && e.target.dataset["index"]) {
            if (this.selected_element) {
                this.selected_element.classList.remove("active");
            }

            e.target.classList.add("active");

            const index = parseInt(e.target.dataset["index"]!, 10);

            this.selected_element = e.target;
            this.selected.val = this.models[index];
        }
    };
}
