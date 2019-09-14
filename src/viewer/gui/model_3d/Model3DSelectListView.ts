import { ResizableWidget } from "../../../core/gui/ResizableWidget";
import { create_element } from "../../../core/gui/dom";
import { WritableProperty } from "../../../core/observable/property/WritableProperty";
import "./Model3DSelectListView.css";

export class Model3DSelectListView<T extends { name: string }> extends ResizableWidget {
    set borders(borders: boolean) {
        if (borders) {
            this.element.style.borderLeft = "var(--border)";
            this.element.style.borderRight = "var(--border)";
        } else {
            this.element.style.borderLeft = "none";
            this.element.style.borderRight = "none";
        }
    }

    private selected_model?: T;
    private selected_element?: HTMLLIElement;

    constructor(private models: T[], private selected: WritableProperty<T | undefined>) {
        super(create_element("ul", { class: "viewer_Model3DSelectListView" }));

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

        this.finalize_construction(Model3DSelectListView.prototype);
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
