import { Widget } from "../../core/gui/Widget";
import { el } from "../../core/gui/dom";
import { Table } from "../../core/gui/Table";
import { hunt_optimizer_stores } from "../stores/HuntOptimizerStore";
import { Disposable } from "../../core/observable/Disposable";
import { list_property } from "../../core/observable";
import { OptimalMethodModel } from "../model";
import { Difficulty } from "../../core/model";
import { Episode } from "../../core/data_formats/parsing/quest/Episode";

export class OptimizationResultView extends Widget {
    private results_observer?: Disposable;

    constructor() {
        super(
            el.div(
                { class: "hunt_optimizer_OptimizationResultView" },
                el.h2({ text: "Optimization Result" }),
            ),
        );

        const optimal_methods = list_property<OptimalMethodModel>();

        this.element.append(
            this.disposable(
                new Table({
                    values: optimal_methods,
                    columns: [
                        {
                            title: "Difficulty",
                            width: 80,
                            create_cell(value: OptimalMethodModel) {
                                return el.td({ text: Difficulty[value.difficulty] });
                            },
                        },
                        {
                            title: "Method",
                            width: 200,
                            create_cell(value: OptimalMethodModel) {
                                return el.td({ text: value.method_name });
                            },
                        },
                        {
                            title: "Ep.",
                            width: 50,
                            create_cell(value: OptimalMethodModel) {
                                return el.td({ text: Episode[value.method_episode] });
                            },
                        },
                    ],
                }),
            ).element,
        );

        this.disposable(
            hunt_optimizer_stores.observe_current(
                hunt_optimizer_store => {
                    if (this.results_observer) {
                        this.results_observer.dispose();
                    }

                    this.results_observer = hunt_optimizer_store.result.observe(
                        ({ value: result }) => {
                            if (result) {
                                optimal_methods.val = result.optimal_methods;
                            } else {
                                optimal_methods.val = [];
                            }
                        },
                        {
                            call_now: true,
                        },
                    );
                },
                { call_now: true },
            ),
        );
    }

    dispose(): void {
        super.dispose();

        if (this.results_observer) {
            this.results_observer.dispose();
        }
    }
}
