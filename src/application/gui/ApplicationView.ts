import { NavigationView } from "./NavigationView";
import { MainContentView } from "./MainContentView";
import { GuiStore, GuiTool } from "../../core/stores/GuiStore";
import { div } from "../../core/gui/dom";
import "./ApplicationView.css";
import { ResizableView } from "../../core/gui/ResizableView";
import { Widget } from "../../core/gui/Widget";
import { Resizable } from "../../core/gui/Resizable";

/**
 * The top-level view which contains all other views.
 */
export class ApplicationView extends ResizableView {
    private menu_view: NavigationView;
    private main_content_view: MainContentView;

    readonly element: HTMLElement;

    constructor(gui_store: GuiStore, tool_views: [GuiTool, () => Promise<Widget & Resizable>][]) {
        super();

        this.menu_view = this.add(new NavigationView(gui_store));
        this.main_content_view = this.add(new MainContentView(gui_store, tool_views));

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
