import "./CharacterClassSelectionView.css";
import { Property } from "../../../core/observable/property/Property";
import { li, ul } from "../../../core/gui/dom";
import { ResizableView } from "../../../core/gui/ResizableView";

export class CharacterClassSelectionView<T extends { name: string }> extends ResizableView {
    private selected_model?: T;
    private selected_element?: HTMLLIElement;

    readonly element = ul({ className: "viewer_model_CharacterClassSelectionView" });

    constructor(
        private character_classes: readonly T[],
        private selected: Property<T | undefined>,
        private set_selected: (selected: T) => void,
        private border_left: boolean,
    ) {
        super();

        this.element.onclick = this.list_click;

        if (border_left) {
            this.element.style.borderLeft = "var(--border)";
        }

        character_classes.forEach((character_class, index) => {
            this.element.append(li({ data: { index: index.toString() } }, character_class.name));
        });

        this.disposables(
            selected.observe(
                ({ value: model }) => {
                    if (this.selected_element) {
                        this.selected_element.classList.remove("active");
                        this.selected_element = undefined;
                    }

                    if (model && model !== this.selected_model) {
                        const index = this.character_classes.indexOf(model);

                        if (index !== -1) {
                            this.selected_element = this.element.childNodes[index] as HTMLLIElement;
                            this.selected_element.classList.add("active");
                        }
                    }
                },
                { call_now: true },
            ),
        );

        this.finalize_construction();
    }

    private list_click = (e: MouseEvent): void => {
        if (e.target instanceof HTMLLIElement && e.target.dataset["index"]) {
            if (this.selected_element) {
                this.selected_element.classList.remove("active");
            }

            e.target.classList.add("active");

            const index = parseInt(e.target.dataset["index"]!, 10);

            this.selected_element = e.target;
            this.set_selected(this.character_classes[index]);
        }
    };
}
