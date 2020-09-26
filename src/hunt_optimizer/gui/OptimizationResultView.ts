import { div, h2, section_id_icon, span } from "../../core/gui/dom";
import { Column, Table } from "../../core/gui/Table";
import { Disposable } from "../../core/observable/Disposable";
import { list_property } from "../../core/observable";
import { OptimalMethodModel, OptimalResultModel } from "../model";
import { Difficulty } from "../../core/model";
import { Episode } from "../../core/data_formats/parsing/quest/Episode";
import "./OptimizationResultView.css";
import { Duration } from "luxon";
import { ServerMap } from "../../core/stores/ServerMap";
import { HuntOptimizerStore } from "../stores/HuntOptimizerStore";
import { LogManager } from "../../core/logging";
import { View } from "../../core/gui/View";

const logger = LogManager.get("hunt_optimizer/gui/OptimizationResultView");

export class OptimizationResultView extends View {
    readonly element = div(
        { className: "hunt_optimizer_OptimizationResultView" },
        h2("Ideal Combination of Methods"),
    );

    private results_observer?: Disposable;
    private table?: Table<OptimalMethodModel>;

    constructor(hunt_optimizer_stores: ServerMap<HuntOptimizerStore>) {
        super();

        this.disposable(
            hunt_optimizer_stores.current.observe(
                async ({ value }) => {
                    try {
                        const hunt_optimizer_store = await value;
                        if (this.disposed) return;

                        if (this.results_observer) {
                            this.remove_disposable(this.results_observer);
                        }

                        this.results_observer = this.disposable(
                            hunt_optimizer_store.result.observe(
                                ({ value }) => this.update_table(value),
                                {
                                    call_now: true,
                                },
                            ),
                        );
                    } catch (e) {
                        logger.error("Couldn't load hunt optimizer store.", e);
                    }
                },
                { call_now: true },
            ),
        );

        this.finalize_construction(OptimizationResultView);
    }

    private update_table(result?: OptimalResultModel): void {
        if (this.table) {
            this.remove(this.table);
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
                    const element = span(
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

        this.table = this.add(
            new Table<OptimalMethodModel>({
                class: "hunt_optimizer_OptimizationResultView_table",
                values: result
                    ? list_property(undefined, ...result.optimal_methods)
                    : list_property(),
                columns,
            }),
        );

        this.element.append(this.table.element);
    }
}
