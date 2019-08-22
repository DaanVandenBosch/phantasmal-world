import { View } from "./View";
import { el } from "./dom";
import { LazyView } from "./LazyView";
import { Resizable } from "./Resizable";
import { ResizableView } from "./ResizableView";
import "./TabContainer.css";

export type Tab = {
    title: string;
    key: string;
    create_view: () => Promise<View & Resizable>;
};

type TabInfo = Tab & { tab_element: HTMLSpanElement; lazy_view: LazyView };

const BAR_HEIGHT = 28;

export class TabContainer extends ResizableView {
    readonly element = el("div", { class: "core_TabContainer" });

    private tabs: TabInfo[] = [];
    private bar_element = el("div", { class: "core_TabContainer_Bar" });
    private panes_element = el("div", { class: "core_TabContainer_Panes" });

    constructor(...tabs: Tab[]) {
        super();

        this.bar_element.onclick = this.bar_click;

        for (const tab of tabs) {
            const tab_element = el("span", {
                class: "core_TabContainer_Tab",
                text: tab.title,
                data: { key: tab.key },
            });
            this.bar_element.append(tab_element);

            const lazy_view = new LazyView(tab.create_view);

            this.tabs.push({
                ...tab,
                tab_element,
                lazy_view,
            });

            this.panes_element.append(lazy_view.element);
            this.disposable(lazy_view);
        }

        if (this.tabs.length) {
            this.activate(this.tabs[0].key);
        }

        this.element.append(this.bar_element, this.panes_element);
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

    private bar_click = (e: MouseEvent) => {
        if (e.target instanceof HTMLElement) {
            const key = e.target.dataset["key"];
            if (key) this.activate(key);
        }
    };

    private activate(key: string): void {
        for (const tab of this.tabs) {
            const active = tab.key === key;

            if (active) {
                tab.tab_element.classList.add("active");
            } else {
                tab.tab_element.classList.remove("active");
            }

            tab.lazy_view.visible = active;
        }
    }
}
