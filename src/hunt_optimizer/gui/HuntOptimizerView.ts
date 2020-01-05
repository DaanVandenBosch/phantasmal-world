import { TabContainer } from "../../core/gui/TabContainer";
import { ServerMap } from "../../core/stores/ServerMap";
import { HuntOptimizerStore } from "../stores/HuntOptimizerStore";
import { HuntMethodStore } from "../stores/HuntMethodStore";
import { GuiStore } from "../../core/stores/GuiStore";
import { ResizableView } from "../../core/gui/ResizableView";

export class HuntOptimizerView extends ResizableView {
    private readonly tab_container: TabContainer;

    get element(): HTMLElement {
        return this.tab_container.element;
    }

    constructor(
        gui_store: GuiStore,
        hunt_optimizer_stores: ServerMap<HuntOptimizerStore>,
        hunt_method_stores: ServerMap<HuntMethodStore>,
    ) {
        super();

        this.tab_container = this.add(
            new TabContainer(gui_store, {
                class: "hunt_optimizer_HuntOptimizerView",
                tabs: [
                    {
                        title: "Optimize",
                        key: "optimize",
                        path: "/optimize",
                        create_view: async function() {
                            return new (await import("./OptimizerView")).OptimizerView(
                                hunt_optimizer_stores,
                            );
                        },
                    },
                    {
                        title: "Methods",
                        key: "methods",
                        path: "/methods",
                        create_view: async function() {
                            return new (await import("./MethodsView")).MethodsView(
                                gui_store,
                                hunt_method_stores,
                            );
                        },
                    },
                    {
                        title: "Help",
                        key: "help",
                        path: "/help",
                        create_view: async function() {
                            return new (await import("./HelpView")).HelpView();
                        },
                    },
                ],
            }),
        );

        this.finalize_construction();
    }

    resize(width: number, height: number): void {
        super.resize(width, height);
        this.tab_container.resize(width, height);
    }
}
