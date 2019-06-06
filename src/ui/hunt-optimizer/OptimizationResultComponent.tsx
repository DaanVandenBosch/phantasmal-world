import { observer } from "mobx-react";
import React from "react";
import { AutoSizer, GridCellRenderer, MultiGrid, Index } from "react-virtualized";
import { Item } from "../../domain";
import { huntOptimizerStore, OptimizationResult } from "../../stores/HuntOptimizerStore";
import "./OptimizationResultComponent.less";
import { computed } from "mobx";

@observer
export class OptimizationResultComponent extends React.Component {
    private standardColumns: Array<{
        title: string,
        width: number,
        cellValue: (result: OptimizationResult) => string,
        className?: string
    }> = [
            {
                title: 'Difficulty',
                width: 75,
                cellValue: (result) => result.difficulty
            },
            {
                title: 'Method',
                width: 200,
                cellValue: (result) => result.methodName
            },
            {
                title: 'Section ID',
                width: 80,
                cellValue: (result) => result.sectionId
            },
            {
                title: 'Hours/Run',
                width: 85,
                cellValue: (result) => result.methodTime.toFixed(1),
                className: 'number'
            },
            {
                title: 'Runs',
                width: 50,
                cellValue: (result) => result.runs.toFixed(1),
                className: 'number'
            },
            {
                title: 'Total Hours',
                width: 90,
                cellValue: (result) => result.totalTime.toFixed(1),
                className: 'number'
            },
        ];

    @computed private get items(): Item[] {
        const items = new Set<Item>();

        for (const r of huntOptimizerStore.result) {
            for (const i of r.itemCounts.keys()) {
                items.add(i);
            }
        }

        return [...items];
    }

    render() {
        // Make sure render is called when result changes.
        huntOptimizerStore.result.slice(0, 0);

        return (
            <section className="ho-OptimizationResultComponent">
                <h3>Optimization Result</h3>
                <div className="ho-OptimizationResultComponent-table">
                    <AutoSizer>
                        {({ width, height }) =>
                            <MultiGrid
                                fixedRowCount={1}
                                width={width}
                                height={height}
                                rowHeight={26}
                                rowCount={1 + huntOptimizerStore.result.length}
                                columnWidth={this.columnWidth}
                                columnCount={this.standardColumns.length + this.items.length}
                                cellRenderer={this.cellRenderer}
                                classNameTopRightGrid="ho-OptimizationResultComponent-table-top-right"
                                noContentRenderer={() =>
                                    <div className="ho-OptimizationResultComponent-no-result">
                                        Add some items and click "Optimize" to see the result here.
                                    </div>
                                }
                            />
                        }
                    </AutoSizer>
                </div>
            </section>
        );
    }

    private columnWidth = ({ index }: Index) => {
        const column = this.standardColumns[index];
        return column ? column.width : 80;
    }

    private cellRenderer: GridCellRenderer = ({ columnIndex, rowIndex, style }) => {
        const column = this.standardColumns[columnIndex];
        let text: string;
        let title: string | undefined;
        const classes = ['ho-OptimizationResultComponent-cell'];

        if (columnIndex === this.standardColumns.length + this.items.length - 1) {
            classes.push('last-in-row');
        }

        if (rowIndex === 0) {
            // Header
            text = title = column
                ? column.title
                : this.items[columnIndex - this.standardColumns.length].name;
        } else {
            // Method row
            const result = huntOptimizerStore.result[rowIndex - 1];

            if (column) {
                text = title = column.cellValue(result);
            } else {
                const itemCount = result.itemCounts.get(
                    this.items[columnIndex - this.standardColumns.length]
                );

                if (itemCount) {
                    text = itemCount.toFixed(2);
                    title = itemCount.toString();
                } else {
                    text = '';
                }
            }

            if (column) {
                if (column.className) {
                    classes.push(column.className);
                }
            } else {
                classes.push('number');
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
