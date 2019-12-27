import { NavigationView } from "./NavigationView";
import { MainContentView } from "./MainContentView";
import { ResizableWidget } from "../../core/gui/ResizableWidget";
import { GuiStore, GuiTool } from "../../core/stores/GuiStore";
import { div } from "../../core/gui/dom";
import "./ApplicationView.css";

/**
 * The top-level view which contains all other views.
 */
export class ApplicationView extends ResizableWidget {
    private menu_view: NavigationView;
    private main_content_view: MainContentView;

    readonly element: HTMLElement;

    constructor(gui_store: GuiStore, tool_views: [GuiTool, () => Promise<ResizableWidget>][]) {
        super();

        this.menu_view = this.disposable(new NavigationView(gui_store));
        this.main_content_view = this.disposable(new MainContentView(gui_store, tool_views));

        this.element = div(
            { className: "application_ApplicationView" },
            this.menu_view.element,
            this.main_content_view.element,
        );
        this.element.id = "root";

        this.finalize_construction();
    }

    resize(width: number, height: number): this {
        super.resize(width, height);
        this.main_content_view.resize(width, height - this.menu_view.height);
        return this;
    }
}
