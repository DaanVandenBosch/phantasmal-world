import { observer } from "mobx-react";
import React from "react";
import { AutoSizer, GridCellRenderer, MultiGrid, Index } from "react-virtualized";
import { HuntMethod } from "../../domain";
import { EnemyNpcTypes } from "../../domain/NpcType";
import { huntMethodStore } from "../../stores/HuntMethodStore";
import "./MethodsComponent.css";

type Column = {
    name: string,
    width: number,
    cellValue: (method: HuntMethod) => string,
    className?: string
}

@observer
export class MethodsComponent extends React.Component {
    static columns: Array<Column> = (() => {
        // Standard columns.
        const columns: Column[] = [
            {
                name: 'Method',
                width: 250,
                cellValue: (method) => method.name,
            },
            {
                name: 'Hours',
                width: 50,
                cellValue: (method) => method.time.toString(),
            },
        ];

        // One column per enemy type.
        for (const enemy of EnemyNpcTypes) {
            columns.push({
                name: enemy.name,
                width: 50,
                cellValue: (method) => {
                    const count = method.enemyCounts.get(enemy);
                    return count == null ? '' : count.toString();
                },
                className: 'number',
            });
        }

        return columns;
    })();

    render() {
        const methods = huntMethodStore.methods.current.value;

        return (
            <section className="ho-MethodsComponent">
                <AutoSizer>
                    {({ width, height }) => (
                        <MultiGrid
                            width={width}
                            height={height}
                            rowHeight={28}
                            rowCount={methods.length}
                            fixedRowCount={1}
                            columnWidth={this.columnWidth}
                            columnCount={2 + EnemyNpcTypes.length}
                            fixedColumnCount={2}
                            cellRenderer={this.cellRenderer}
                        />
                    )}
                </AutoSizer>
            </section>
        );
    }

    private columnWidth = ({ index }: Index): number => {
        return MethodsComponent.columns[index].width;
    }

    private cellRenderer: GridCellRenderer = ({ columnIndex, rowIndex, style }) => {
        const column = MethodsComponent.columns[columnIndex];
        let text: string;
        const classes = [];

        if (columnIndex === MethodsComponent.columns.length - 1) {
            classes.push('last-in-row');
        }

        if (rowIndex === 0) {
            // Header row
            text = column.name;
        } else {
            // Method row
            if (column.className) {
                classes.push(column.className);
            }

            const method = huntMethodStore.methods.current.value[rowIndex - 1];

            text = column.cellValue(method);
        }

        return (
            <div
                className={classes.join(' ')}
                key={`${columnIndex}, ${rowIndex}`}
                style={style}
            >
                <span>{text}</span>
            </div>
        );
    }
}