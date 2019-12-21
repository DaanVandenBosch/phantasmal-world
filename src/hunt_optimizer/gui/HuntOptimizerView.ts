import { TabContainer } from "../../core/gui/TabContainer";
import { ServerMap } from "../../core/stores/ServerMap";
import { HuntOptimizerStore } from "../stores/HuntOptimizerStore";
import { HuntMethodStore } from "../stores/HuntMethodStore";

export class HuntOptimizerView extends TabContainer {
    constructor(
        hunt_optimizer_stores: ServerMap<HuntOptimizerStore>,
        hunt_method_stores: ServerMap<HuntMethodStore>,
    ) {
        super({
            class: "hunt_optimizer_HuntOptimizerView",
            tabs: [
                {
                    title: "Optimize",
                    key: "optimize",
                    create_view: async function() {
                        return new (await import("./OptimizerView")).OptimizerView(
                            hunt_optimizer_stores,
                        );
                    },
                },
                {
                    title: "Methods",
                    key: "methods",
                    create_view: async function() {
                        return new (await import("./MethodsView")).MethodsView(hunt_method_stores);
                    },
                },
                {
                    title: "Help",
                    key: "help",
                    create_view: async function() {
                        return new (await import("./HelpView")).HelpView();
                    },
                },
            ],
        });

        this.finalize_construction();
    }
}
