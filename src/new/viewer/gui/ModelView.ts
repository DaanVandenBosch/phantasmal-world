import { create_el } from "../../core/gui/dom";
import { ResizableView } from "../../core/gui/ResizableView";
import { ToolBar } from "../../core/gui/ToolBar";
import "./ModelView.css";
import { model_store } from "../stores/ModelStore";
import { Property } from "../../core/observable/Property";
import { RendererView } from "../../core/gui/RendererView";
import { ModelRenderer } from "../rendering/ModelRenderer";
import { View } from "../../core/gui/View";
import { FileInput } from "../../core/gui/FileInput";
import { CheckBox } from "../../core/gui/CheckBox";

const MODEL_LIST_WIDTH = 100;
const ANIMATION_LIST_WIDTH = 150;

export class ModelView extends ResizableView {
    element = create_el("div", "viewer_ModelView");

    private tool_bar_view = this.disposable(new ToolBarView());
    private container_element = create_el("div", "viewer_ModelView_container");
    private model_list_view = this.disposable(
        new ModelSelectListView(model_store.models, model_store.current_model),
    );
    private animation_list_view = this.disposable(
        new ModelSelectListView(model_store.animations, model_store.current_animation),
    );
    private renderer_view = this.disposable(new RendererView(new ModelRenderer()));

    constructor() {
        super();

        this.animation_list_view.borders = true;

        this.container_element.append(
            this.model_list_view.element,
            this.animation_list_view.element,
            this.renderer_view.element,
        );

        this.element.append(this.tool_bar_view.element, this.container_element);

        model_store.current_model.set(model_store.models[5]);
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

class ToolBarView extends View {
    private readonly open_file_button = new FileInput("Open file...", ".nj, .njm, .xj");
    private readonly skeleton_checkbox = new CheckBox("Show skeleton");

    private readonly tool_bar = this.disposable(
        new ToolBar(this.open_file_button, this.skeleton_checkbox),
    );

    readonly element = this.tool_bar.element;

    get height(): number {
        return this.tool_bar.height;
    }

    constructor() {
        super();

        this.disposable(
            this.open_file_button.files.observe(files => {
                if (files.length) model_store.load_file(files[0]);
            }),
        );

        this.disposable(
            this.skeleton_checkbox.checked.observe(checked =>
                model_store.show_skeleton.set(checked),
            ),
        );
    }
}

class ModelSelectListView<T extends { name: string }> extends ResizableView {
    element = create_el("ul", "viewer_ModelSelectListView");

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

    constructor(private models: T[], private selected: Property<T | undefined>) {
        super();

        this.element.onclick = this.list_click;

        models.forEach((model, index) => {
            this.element.append(
                create_el("li", undefined, li => {
                    li.textContent = model.name;
                    li.dataset["index"] = index.toString();
                }),
            );
        });

        this.disposable(
            selected.observe(model => {
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
            this.selected.set(this.models[index]);
        }
    };
}
