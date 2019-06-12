import React from "react";
import { Index, MultiGrid, GridCellRenderer } from "react-virtualized";
import "./dataTable.less";

export type Column<T> = {
    name: string,
    width: number,
    cellValue: (record: T) => string,
    tooltip?: (record: T) => string,
    footerValue?: string,
    footerTooltip?: string,
    className?: string,
}

/**
 * A table with a fixed header. Optionally has fixed columns and a footer.
 * TODO: no-content message.
 */
export class DataTable<T> extends React.Component<{
    width: number,
    height: number,
    rowCount: number,
    columns: Array<Column<T>>,
    fixedColumnCount?: number,
    record: (index: Index) => T,
    footer?: boolean,
}> {
    render() {
        return (
            <div className="DataTable">
                <MultiGrid
                    width={this.props.width}
                    height={this.props.height}
                    rowHeight={26}
                    rowCount={this.props.rowCount + 1 + (this.props.footer ? 1 : 0)}
                    fixedRowCount={1}
                    columnWidth={this.columnWidth}
                    columnCount={this.props.columns.length}
                    fixedColumnCount={this.props.fixedColumnCount}
                    cellRenderer={this.cellRenderer}
                    classNameTopLeftGrid="DataTable-header"
                    classNameTopRightGrid="DataTable-header"
                />
            </div>
        );
    }

    private columnWidth = ({ index }: Index): number => {
        return this.props.columns[index].width;
    }

    private cellRenderer: GridCellRenderer = ({ columnIndex, rowIndex, style }) => {
        const column = this.props.columns[columnIndex];
        let text: string;
        let title: string | undefined;
        const classes = ['DataTable-cell'];

        if (columnIndex === this.props.columns.length - 1) {
            classes.push('last-in-row');
        }

        if (rowIndex === 0) {
            // Header row
            text = title = column.name;
        } else {
            // Record or footer row
            if (column.className) {
                classes.push(column.className);
            }

            if (this.props.footer && rowIndex === 1 + this.props.rowCount) {
                // Footer row
                classes.push('footer-cell');
                text = column.footerValue == null ? '' : column.footerValue;
                title = column.footerTooltip == null ? '' : column.footerTooltip;
            } else {
                // Record row
                const result = this.props.record({ index: rowIndex - 1 });

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
