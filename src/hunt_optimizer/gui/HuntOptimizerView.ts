import { TabContainer } from "../../core/gui/TabContainer";

export class HuntOptimizerView extends TabContainer {
    constructor() {
        super({
            class: "hunt_optimizer_HuntOptimizerView",
            tabs: [
                {
                    title: "Optimize",
                    key: "optimize",
                    create_view: async function() {
                        return new (await import("./OptimizerView")).OptimizerView();
                    },
                },
                {
                    title: "Methods",
                    key: "methods",
                    create_view: async function() {
                        return new (await import("./MethodsView")).MethodsView();
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
    }
}
