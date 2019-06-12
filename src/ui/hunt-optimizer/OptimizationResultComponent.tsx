import { computed } from "mobx";
import { observer } from "mobx-react";
import React from "react";
import { AutoSizer, Index } from "react-virtualized";
import { Item } from "../../domain";
import { huntOptimizerStore, OptimizationResult } from "../../stores/HuntOptimizerStore";
import { Column, DataTable } from "../dataTable";
import "./OptimizationResultComponent.less";
import { hoursToString } from "../time";

@observer
export class OptimizationResultComponent extends React.Component {
    @computed private get columns(): Column<OptimizationResult>[] {
        // Standard columns.
        const results = huntOptimizerStore.results;
        let totalRuns = 0;
        let totalTime = 0;

        for (const result of results) {
            totalRuns += result.runs;
            totalTime += result.totalTime;
        }

        const columns: Column<OptimizationResult>[] = [
            {
                name: 'Difficulty',
                width: 75,
                cellValue: (result) => result.difficulty,
                footerValue: 'Totals:',
            },
            {
                name: 'Method',
                width: 200,
                cellValue: (result) => result.methodName,
                tooltip: (result) => result.methodName,
            },
            {
                name: 'Section ID',
                width: 80,
                cellValue: (result) => result.sectionId,
            },
            {
                name: 'Time/Run',
                width: 80,
                cellValue: (result) => hoursToString(result.methodTime),
                className: 'number',
            },
            {
                name: 'Runs',
                width: 60,
                cellValue: (result) => result.runs.toFixed(1),
                tooltip: (result) => result.runs.toString(),
                footerValue: totalRuns.toFixed(1),
                footerTooltip: totalRuns.toString(),
                className: 'number',
            },
            {
                name: 'Total Hours',
                width: 90,
                cellValue: (result) => result.totalTime.toFixed(1),
                tooltip: (result) => result.totalTime.toString(),
                footerValue: totalTime.toFixed(1),
                footerTooltip: totalTime.toString(),
                className: 'number',
            },
        ];

        // Add one column per item.
        const items = new Set<Item>();

        for (const r of results) {
            for (const i of r.itemCounts.keys()) {
                items.add(i);
            }
        }

        for (const item of items) {
            const totalCount = results.reduce(
                (acc, r) => acc + (r.itemCounts.get(item) || 0),
                0
            );

            columns.push({
                name: item.name,
                width: 80,
                cellValue: (result) => {
                    const count = result.itemCounts.get(item);
                    return count ? count.toFixed(2) : '';
                },
                tooltip: (result) => {
                    const count = result.itemCounts.get(item);
                    return count ? count.toString() : '';
                },
                className: 'number',
                footerValue: totalCount.toFixed(2),
                footerTooltip: totalCount.toString()
            });
        }

        return columns;
    }

    render() {
        // Make sure render is called when result changes.
        huntOptimizerStore.results.slice(0, 0);

        return (
            <section className="ho-OptimizationResultComponent">
                <h3>Optimization Result</h3>
                <div className="ho-OptimizationResultComponent-table">
                    <AutoSizer>
                        {({ width, height }) =>
                            <DataTable
                                width={width}
                                height={height}
                                rowCount={huntOptimizerStore.results.length}
                                columns={this.columns}
                                fixedColumnCount={3}
                                record={this.record}
                                footer={huntOptimizerStore.results.length > 0}
                            />
                        }
                    </AutoSizer>
                </div>
            </section>
        );
    }

    private record = ({ index }: Index): OptimizationResult => {
        return huntOptimizerStore.results[index];
    }
}
