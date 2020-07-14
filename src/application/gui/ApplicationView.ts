import { NavigationView } from "./NavigationView";
import { MainContentView } from "./MainContentView";
import { div } from "../../core/gui/dom";
import "./ApplicationView.css";
import { ResizableView } from "../../core/gui/ResizableView";

/**
 * The top-level view which contains all other views.
 */
export class ApplicationView extends ResizableView {
    readonly element: HTMLElement;

    constructor(
        private readonly navigation_view: NavigationView,
        private readonly main_content_view: MainContentView,
    ) {
        super();

        this.element = div(
            { className: "application_ApplicationView" },
            this.navigation_view.element,
            this.main_content_view.element,
        );
        this.element.id = "root";

        this.add(navigation_view);
        this.add(main_content_view);

        this.finalize_construction(ApplicationView);
    }

    resize(width: number, height: number): this {
        super.resize(width, height);
        this.main_content_view.resize(width, height - this.navigation_view.height);
        return this;
    }
}
