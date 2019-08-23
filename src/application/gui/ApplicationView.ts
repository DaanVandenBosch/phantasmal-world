import { NavigationView } from "./NavigationView";
import { MainContentView } from "./MainContentView";
import { create_element } from "../../core/gui/dom";
import { ResizableView } from "../../core/gui/ResizableView";

export class ApplicationView extends ResizableView {
    element = create_element("div", { class: "application_ApplicationView" });

    private menu_view = this.disposable(new NavigationView());
    private main_content_view = this.disposable(new MainContentView());

    constructor() {
        super();

        this.element.id = "root";

        this.element.append(this.menu_view.element, this.main_content_view.element);
    }

    resize(width: number, height: number): this {
        super.resize(width, height);
        this.main_content_view.resize(width, height - this.menu_view.height);
        return this;
    }
}
