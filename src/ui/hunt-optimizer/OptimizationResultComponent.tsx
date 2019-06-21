import { computed } from "mobx";
import { observer } from "mobx-react";
import React from "react";
import { AutoSizer, Index } from "react-virtualized";
import { Difficulty, Episode, SectionId } from "../../domain";
import { huntOptimizerStore, OptimalMethod } from "../../stores/HuntOptimizerStore";
import { BigTable, Column } from "../BigTable";
import { SectionIdIcon } from "../SectionIdIcon";
import { hoursToString } from "../time";
import "./OptimizationResultComponent.less";

@observer
export class OptimizationResultComponent extends React.Component {
    @computed private get columns(): Column<OptimalMethod>[] {
        // Standard columns.
        const result = huntOptimizerStore.result;
        const optimalMethods = result ? result.optimalMethods : [];
        let totalRuns = 0;
        let totalTime = 0;

        for (const method of optimalMethods) {
            totalRuns += method.runs;
            totalTime += method.totalTime;
        }

        const columns: Column<OptimalMethod>[] = [
            {
                name: 'Difficulty',
                width: 75,
                cellRenderer: (result) => Difficulty[result.difficulty],
                footerValue: 'Totals:',
            },
            {
                name: 'Method',
                width: 200,
                cellRenderer: (result) => result.methodName,
                tooltip: (result) => result.methodName,
            },
            {
                name: 'Ep.',
                width: 34,
                cellRenderer: (result) => Episode[result.methodEpisode],
            },
            {
                name: 'Section ID',
                width: 80,
                cellRenderer: (result) => (
                    <div className="ho-OptimizationResultComponent-sid-col">
                        {result.sectionIds.map(sid =>
                            <SectionIdIcon sectionId={sid} key={sid} size={20} />
                        )}
                    </div>
                ),
                tooltip: (result) => result.sectionIds.map(sid => SectionId[sid]).join(', '),
            },
            {
                name: 'Time/Run',
                width: 80,
                cellRenderer: (result) => hoursToString(result.methodTime),
                className: 'number',
            },
            {
                name: 'Runs',
                width: 60,
                cellRenderer: (result) => result.runs.toFixed(1),
                tooltip: (result) => result.runs.toString(),
                footerValue: totalRuns.toFixed(1),
                footerTooltip: totalRuns.toString(),
                className: 'number',
            },
            {
                name: 'Total Hours',
                width: 90,
                cellRenderer: (result) => result.totalTime.toFixed(1),
                tooltip: (result) => result.totalTime.toString(),
                footerValue: totalTime.toFixed(1),
                footerTooltip: totalTime.toString(),
                className: 'number',
            },
        ];

        // Add one column per item.
        if (result) {
            for (const item of result.wantedItems) {
                let totalCount = 0;

                for (const method of optimalMethods) {
                    totalCount += method.itemCounts.get(item) || 0;
                }

                columns.push({
                    name: item.name,
                    width: 80,
                    cellRenderer: (result) => {
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
        }

        return columns;
    }

    // Make sure render is called when result changes.
    @computed private get updateTrigger() {
        return huntOptimizerStore.result;
    }

    render() {
        this.updateTrigger; // eslint-disable-line
        const result = huntOptimizerStore.result;

        return (
            <section className="ho-OptimizationResultComponent">
                <h3>Optimization Result</h3>
                <div className="ho-OptimizationResultComponent-table">
                    <AutoSizer>
                        {({ width, height }) =>
                            <BigTable
                                width={width}
                                height={height}
                                rowCount={result ? result.optimalMethods.length : 0}
                                columns={this.columns}
                                fixedColumnCount={3}
                                record={this.record}
                                footer={result != null}
                                updateTrigger={this.updateTrigger}
                            />
                        }
                    </AutoSizer>
                </div>
            </section>
        );
    }

    private record = ({ index }: Index): OptimalMethod => {
        return huntOptimizerStore.result!.optimalMethods[index];
    }
}
