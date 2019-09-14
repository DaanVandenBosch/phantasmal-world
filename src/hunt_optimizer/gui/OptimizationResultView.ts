import { Widget } from "../../core/gui/Widget";
import { el, section_id_icon } from "../../core/gui/dom";
import { Column, Table } from "../../core/gui/Table";
import { hunt_optimizer_stores } from "../stores/HuntOptimizerStore";
import { Disposable } from "../../core/observable/Disposable";
import { list_property } from "../../core/observable";
import { OptimalMethodModel, OptimalResultModel } from "../model";
import { Difficulty } from "../../core/model";
import { Episode } from "../../core/data_formats/parsing/quest/Episode";
import "./OptimizationResultView.css";
import { Duration } from "luxon";

export class OptimizationResultView extends Widget {
    private results_observer?: Disposable;
    private table?: Table<OptimalMethodModel>;

    constructor() {
        super(
            el.div(
                { class: "hunt_optimizer_OptimizationResultView" },
                el.h2({ text: "Ideal Combination of Methods" }),
            ),
        );

        this.disposable(
            hunt_optimizer_stores.observe_current(
                hunt_optimizer_store => {
                    if (this.results_observer) {
                        this.results_observer.dispose();
                    }

                    this.results_observer = hunt_optimizer_store.result.observe(
                        ({ value }) => this.update_table(value),
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

        if (this.table) {
            this.table.dispose();
        }
    }

    private update_table(result?: OptimalResultModel): void {
        if (this.table) {
            this.table.dispose();
        }

        let total_runs = 0;
        let total_time = Duration.fromMillis(0);

        if (result) {
            for (const method of result.optimal_methods) {
                total_runs += method.runs;
                total_time = total_time.plus(method.total_time);
            }
        }

        const columns: Column<OptimalMethodModel>[] = [
            {
                title: "Difficulty",
                fixed: true,
                width: 80,
                render_cell(value: OptimalMethodModel) {
                    return Difficulty[value.difficulty];
                },
                footer: {
                    render_cell() {
                        return "Totals:";
                    },
                },
            },
            {
                title: "Method",
                fixed: true,
                width: 250,
                render_cell(value: OptimalMethodModel) {
                    return value.method_name;
                },
            },
            {
                title: "Ep.",
                fixed: true,
                width: 40,
                render_cell(value: OptimalMethodModel) {
                    return Episode[value.method_episode];
                },
            },
            {
                title: "Section ID",
                fixed: true,
                width: 90,
                render_cell(value: OptimalMethodModel) {
                    const element = el.span(
                        {},
                        ...value.section_ids.map(sid => section_id_icon(sid, { size: 17 })),
                    );
                    element.style.display = "flex";
                    return element;
                },
            },
            {
                title: "Time/Run",
                width: 90,
                text_align: "center",
                render_cell(value: OptimalMethodModel) {
                    return value.method_time.toFormat("hh:mm");
                },
            },
            {
                title: "Runs",
                width: 60,
                text_align: "right",
                render_cell(value: OptimalMethodModel) {
                    return value.runs.toFixed(1);
                },
                tooltip(value: OptimalMethodModel) {
                    return value.runs.toString();
                },
                footer: {
                    render_cell() {
                        return total_runs.toFixed(1);
                    },
                    tooltip() {
                        return total_runs.toString();
                    },
                },
            },
            {
                title: "Total Hours",
                width: 60,
                text_align: "right",
                render_cell(value: OptimalMethodModel) {
                    return value.total_time.as("hours").toFixed(1);
                },
                tooltip(value: OptimalMethodModel) {
                    return value.total_time.as("hours").toString();
                },
                footer: {
                    render_cell() {
                        return total_time.as("hours").toFixed(1);
                    },
                    tooltip() {
                        return total_time.as("hours").toString();
                    },
                },
            },
        ];

        if (result) {
            for (const item of result.wanted_items) {
                let total_count = 0;

                for (const method of result.optimal_methods) {
                    total_count += method.item_counts.get(item) || 0;
                }

                columns.push({
                    title: item.name,
                    width: 80,
                    text_align: "right",
                    render_cell(value: OptimalMethodModel) {
                        const count = value.item_counts.get(item);
                        return count ? count.toFixed(2) : "";
                    },
                    tooltip(value: OptimalMethodModel) {
                        const count = value.item_counts.get(item);
                        return count ? count.toString() : "";
                    },
                    footer: {
                        render_cell() {
                            return total_count.toFixed(2);
                        },
                        tooltip() {
                            return total_count.toString();
                        },
                    },
                });
            }
        }

        this.table = new Table({
            class: "hunt_optimizer_OptimizationResultView_table",
            values: result ? list_property(undefined, ...result.optimal_methods) : list_property(),
            columns,
        });

        this.element.append(this.table.element);
    }
}
