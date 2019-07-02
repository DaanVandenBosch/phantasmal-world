import { computed } from "mobx";
import { observer } from "mobx-react";
import React, { Component, ReactNode } from "react";
import { AutoSizer, Index } from "react-virtualized";
import { Difficulty, Episode, SectionId } from "../../domain";
import { hunt_optimizer_store, OptimalMethod } from "../../stores/HuntOptimizerStore";
import { BigTable, Column } from "../BigTable";
import { SectionIdIcon } from "../SectionIdIcon";
import { hours_to_string } from "../time";
import "./OptimizationResultComponent.less";

@observer
export class OptimizationResultComponent extends Component {
    @computed private get columns(): Column<OptimalMethod>[] {
        // Standard columns.
        const result = hunt_optimizer_store.result;
        const optimal_methods = result ? result.optimal_methods : [];
        let total_runs = 0;
        let total_time = 0;

        for (const method of optimal_methods) {
            total_runs += method.runs;
            total_time += method.total_time;
        }

        const columns: Column<OptimalMethod>[] = [
            {
                name: "Difficulty",
                width: 75,
                cell_renderer: result => Difficulty[result.difficulty],
                footer_value: "Totals:",
            },
            {
                name: "Method",
                width: 200,
                cell_renderer: result => result.method_name,
                tooltip: result => result.method_name,
            },
            {
                name: "Ep.",
                width: 34,
                cell_renderer: result => Episode[result.method_episode],
            },
            {
                name: "Section ID",
                width: 80,
                cell_renderer: result => (
                    <div className="ho-OptimizationResultComponent-sid-col">
                        {result.section_ids.map(sid => (
                            <SectionIdIcon section_id={sid} key={sid} size={20} />
                        ))}
                    </div>
                ),
                tooltip: result => result.section_ids.map(sid => SectionId[sid]).join(", "),
            },
            {
                name: "Time/Run",
                width: 80,
                cell_renderer: result => hours_to_string(result.method_time),
                class_name: "number",
            },
            {
                name: "Runs",
                width: 60,
                cell_renderer: result => result.runs.toFixed(1),
                tooltip: result => result.runs.toString(),
                footer_value: total_runs.toFixed(1),
                footer_tooltip: total_runs.toString(),
                class_name: "number",
            },
            {
                name: "Total Hours",
                width: 90,
                cell_renderer: result => result.total_time.toFixed(1),
                tooltip: result => result.total_time.toString(),
                footer_value: total_time.toFixed(1),
                footer_tooltip: total_time.toString(),
                class_name: "number",
            },
        ];

        // Add one column per item.
        if (result) {
            for (const item of result.wanted_items) {
                let totalCount = 0;

                for (const method of optimal_methods) {
                    totalCount += method.item_counts.get(item) || 0;
                }

                columns.push({
                    name: item.name,
                    width: 80,
                    cell_renderer: result => {
                        const count = result.item_counts.get(item);
                        return count ? count.toFixed(2) : "";
                    },
                    tooltip: result => {
                        const count = result.item_counts.get(item);
                        return count ? count.toString() : "";
                    },
                    class_name: "number",
                    footer_value: totalCount.toFixed(2),
                    footer_tooltip: totalCount.toString(),
                });
            }
        }

        return columns;
    }

    // Make sure render is called when result changes.
    @computed private get update_trigger(): any {
        return hunt_optimizer_store.result;
    }

    render(): ReactNode {
        this.update_trigger; // eslint-disable-line
        const result = hunt_optimizer_store.result;

        return (
            <section className="ho-OptimizationResultComponent">
                <h3>Optimization Result</h3>
                <div className="ho-OptimizationResultComponent-table">
                    <AutoSizer>
                        {({ width, height }) => (
                            <BigTable
                                width={width}
                                height={height}
                                row_count={result ? result.optimal_methods.length : 0}
                                columns={this.columns}
                                fixed_column_count={4}
                                record={this.record}
                                footer={result != null}
                                update_trigger={this.update_trigger}
                            />
                        )}
                    </AutoSizer>
                </div>
            </section>
        );
    }

    private record = ({ index }: Index): OptimalMethod => {
        return hunt_optimizer_store.result!.optimal_methods[index];
    };
}
