import { Widget, WidgetOptions } from "./Widget";
import { LazyWidget } from "./LazyWidget";
import { ResizableWidget } from "./ResizableWidget";
import "./TabContainer.css";
import { div, span } from "./dom";
import { GuiStore } from "../stores/GuiStore";
import { Resizable } from "./Resizable";

export type Tab = {
    title: string;
    key: string;
    path?: string;
    create_view: () => Promise<Widget & Resizable>;
};

export type TabContainerOptions = WidgetOptions & {
    tabs: Tab[];
};

type TabInfo = Tab & { tab_element: HTMLSpanElement; lazy_view: LazyWidget };

const BAR_HEIGHT = 28;

export class TabContainer extends ResizableWidget {
    private tabs: TabInfo[] = [];
    private bar_element = div({ className: "core_TabContainer_Bar" });
    private panes_element = div({ className: "core_TabContainer_Panes" });
    private active_tab?: TabInfo;

    readonly element = div({ className: "core_TabContainer" });

    get children(): readonly Widget[] {
        return this.tabs.flatMap(tab => tab.lazy_view.children);
    }

    constructor(private readonly gui_store: GuiStore, options: TabContainerOptions) {
        super(options);

        this.bar_element.onmousedown = this.bar_mousedown;

        for (const tab of options.tabs) {
            const tab_element = span(
                {
                    className: "core_TabContainer_Tab",
                    data: { key: tab.key },
                },
                tab.title,
            );
            this.bar_element.append(tab_element);

            const lazy_view = this.disposable(new LazyWidget(tab.create_view));

            const tab_info: TabInfo = {
                ...tab,
                tab_element,
                lazy_view,
            };
            this.tabs.push(tab_info);

            this.panes_element.append(lazy_view.element);
        }

        this.element.append(this.bar_element, this.panes_element);

        this.finalize_construction();
    }

    resize(width: number, height: number): this {
        super.resize(width, height);

        this.bar_element.style.width = `${width}px`;
        this.bar_element.style.height = `${BAR_HEIGHT}px`;

        const tab_pane_height = height - BAR_HEIGHT;

        this.panes_element.style.width = `${width}px`;
        this.panes_element.style.height = `${tab_pane_height}px`;

        for (const tabs of this.tabs) {
            tabs.lazy_view.resize(width, tab_pane_height);
        }

        return this;
    }

    activate(): void {
        if (this.active_tab) {
            this.activate_tab(this.active_tab);
        } else {
            let active_tab: TabInfo | undefined;

            for (const tab_info of this.tabs) {
                if (
                    tab_info.path != undefined &&
                    this.gui_store.path.val.startsWith(tab_info.path)
                ) {
                    active_tab = tab_info;
                }
            }

            if (active_tab) {
                this.activate_tab(active_tab);
            } else if (this.tabs.length) {
                this.activate_tab(this.tabs[0]);
            }
        }
    }

    private bar_mousedown = (e: MouseEvent): void => {
        if (e.target instanceof HTMLElement) {
            const key = e.target.dataset["key"];
            if (key) this.activate_key(key);
        }
    };

    private activate_key(key: string): void {
        for (const tab of this.tabs) {
            if (tab.key === key) {
                this.activate_tab(tab);
                break;
            }
        }
    }

    private activate_tab(tab: TabInfo): void {
        if (this.active_tab !== tab) {
            if (this.active_tab) {
                this.active_tab.tab_element.classList.remove("active");
                this.active_tab.lazy_view.visible.val = false;
                this.active_tab.lazy_view.deactivate();
            }

            this.active_tab = tab;
            tab.tab_element.classList.add("active");
            tab.lazy_view.visible.val = true;
        }

        if (tab.path != undefined) {
            this.gui_store.set_path_prefix(tab.path);
            tab.lazy_view.activate();
        }
    }
}
