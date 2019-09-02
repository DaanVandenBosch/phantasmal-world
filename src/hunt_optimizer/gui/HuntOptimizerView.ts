import { TabContainer } from "../../core/gui/TabContainer";

export class HuntOptimizerView extends TabContainer {
    constructor() {
        super(
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
        );
    }
}
