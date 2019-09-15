import { NavigationView } from "./NavigationView";
import { MainContentView } from "./MainContentView";
import { el } from "../../core/gui/dom";
import { ResizableWidget } from "../../core/gui/ResizableWidget";

export class ApplicationView extends ResizableWidget {
    private menu_view = this.disposable(new NavigationView());
    private main_content_view = this.disposable(new MainContentView());

    readonly element = el.div(
        { class: "application_ApplicationView" },
        this.menu_view.element,
        this.main_content_view.element,
    );

    constructor() {
        super();

        this.element.id = "root";

        this.finalize_construction(ApplicationView.prototype);
    }

    resize(width: number, height: number): this {
        super.resize(width, height);
        this.main_content_view.resize(width, height - this.menu_view.height);
        return this;
    }
}
