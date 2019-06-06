import { observer } from "mobx-react";
import React from "react";
import { AutoSizer, GridCellRenderer, MultiGrid, Index } from "react-virtualized";
import { Item } from "../../domain";
import { huntOptimizerStore, OptimizationResult } from "../../stores/HuntOptimizerStore";
import "./OptimizationResultComponent.less";
import { computed } from "mobx";

type Column = {
    name: string,
    width: number,
    cellValue: (result: OptimizationResult) => string,
    tooltip?: (result: OptimizationResult) => string,
    total?: string,
    totalTooltip?: string,
    className?: string
}

@observer
export class OptimizationResultComponent extends React.Component {
    @computed private get columns(): Column[] {
        // Standard columns.
        const results = huntOptimizerStore.results;
        let totalRuns = 0;
        let totalTime = 0;

        for (const result of results) {
            totalRuns += result.runs;
            totalTime += result.totalTime;
        }

        const columns: Column[] = [
            {
                name: 'Difficulty',
                width: 75,
                cellValue: (result) => result.difficulty,
                total: 'Totals:',
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
                name: 'Hours/Run',
                width: 85,
                cellValue: (result) => result.methodTime.toFixed(1),
                tooltip: (result) => result.methodTime.toString(),
                className: 'number',
            },
            {
                name: 'Runs',
                width: 60,
                cellValue: (result) => result.runs.toFixed(1),
                tooltip: (result) => result.runs.toString(),
                total: totalRuns.toFixed(1),
                totalTooltip: totalRuns.toString(),
                className: 'number',
            },
            {
                name: 'Total Hours',
                width: 90,
                cellValue: (result) => result.totalTime.toFixed(1),
                tooltip: (result) => result.totalTime.toString(),
                total: totalTime.toFixed(1),
                totalTooltip: totalTime.toString(),
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
                total: totalCount.toFixed(2),
                totalTooltip: totalCount.toString()
            });
        }

        return columns;
    }

    render() {
        // Make sure render is called when result changes.
        huntOptimizerStore.results.slice(0, 0);
        // Always add a row for the header. Add a row for the totals only if we have results.
        const rowCount = huntOptimizerStore.results.length
            ? 2 + huntOptimizerStore.results.length
            : 1;

        return (
            <section className="ho-OptimizationResultComponent">
                <h3>Optimization Result</h3>
                <div className="ho-OptimizationResultComponent-table">
                    <AutoSizer>
                        {({ width, height }) =>
                            <MultiGrid
                                width={width}
                                height={height}
                                rowHeight={26}
                                rowCount={rowCount}
                                fixedRowCount={1}
                                columnWidth={this.columnWidth}
                                columnCount={this.columns.length}
                                fixedColumnCount={3}
                                cellRenderer={this.cellRenderer}
                                classNameTopLeftGrid="ho-OptimizationResultComponent-table-header"
                                classNameTopRightGrid="ho-OptimizationResultComponent-table-header"
                            />
                        }
                    </AutoSizer>
                </div>
            </section>
        );
    }

    private columnWidth = ({ index }: Index): number => {
        return this.columns[index].width;
    }

    private cellRenderer: GridCellRenderer = ({ columnIndex, rowIndex, style }) => {
        const column = this.columns[columnIndex];
        let text: string;
        let title: string | undefined;
        const classes = ['ho-OptimizationResultComponent-cell'];

        if (columnIndex === this.columns.length - 1) {
            classes.push('last-in-row');
        }

        if (rowIndex === 0) {
            // Header row
            text = title = column.name;
        } else {
            // Method or totals row
            if (column.className) {
                classes.push(column.className);
            }

            if (rowIndex === 1 + huntOptimizerStore.results.length) {
                // Totals row
                text = column.total == null ? '' : column.total;
                title = column.totalTooltip == null ? '' : column.totalTooltip;
            } else {
                // Method row
                const result = huntOptimizerStore.results[rowIndex - 1];

                text = column.cellValue(result);

                if (column.tooltip) {
                    title = column.tooltip(result);
                }
            }
        }

        return (
            <div
                className={classes.join(' ')}
                key={`${columnIndex}, ${rowIndex}`}
                style={style}
                title={title}
            >
                <span>{text}</span>
            </div>
        );
    }
}
