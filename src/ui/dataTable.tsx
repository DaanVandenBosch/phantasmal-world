import React, { ReactNode } from "react";
import { GridCellRenderer, Index, MultiGrid } from "react-virtualized";
import "./dataTable.less";

export type Column<T> = {
    name: string,
    width: number,
    cellRenderer: (record: T) => ReactNode,
    tooltip?: (record: T) => string,
    footerValue?: string,
    footerTooltip?: string,
    /**
     * "number" and "integrated" have special meaning.
     */
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
    overscanRowCount?: number,
    columns: Array<Column<T>>,
    fixedColumnCount?: number,
    overscanColumnCount?: number,
    record: (index: Index) => T,
    footer?: boolean,
    /**
     * When this changes, the DataTable will re-render.
     */
    updateTrigger?: any
}> {
    render() {
        return (
            <div
                className="DataTable"
                style={{ width: this.props.width, height: this.props.height }}
            >
                <MultiGrid
                    width={this.props.width}
                    height={this.props.height}
                    rowHeight={26}
                    rowCount={this.props.rowCount + 1 + (this.props.footer ? 1 : 0)}
                    fixedRowCount={1}
                    overscanRowCount={this.props.overscanRowCount}
                    columnWidth={this.columnWidth}
                    columnCount={this.props.columns.length}
                    fixedColumnCount={this.props.fixedColumnCount}
                    overscanColumnCount={this.props.overscanColumnCount}
                    cellRenderer={this.cellRenderer}
                    classNameTopLeftGrid="DataTable-header"
                    classNameTopRightGrid="DataTable-header"
                    updateTigger={this.props.updateTrigger}
                />
            </div>
        );
    }

    private columnWidth = ({ index }: Index): number => {
        return this.props.columns[index].width;
    }

    private cellRenderer: GridCellRenderer = ({ columnIndex, rowIndex, style }) => {
        const column = this.props.columns[columnIndex];
        let cell: ReactNode;
        let title: string | undefined;
        const classes = ['DataTable-cell'];

        if (columnIndex === this.props.columns.length - 1) {
            classes.push('last-in-row');
        }

        if (rowIndex === 0) {
            // Header row
            cell = title = column.name;
        } else {
            // Record or footer row
            if (column.className) {
                classes.push(column.className);
            }

            if (this.props.footer && rowIndex === 1 + this.props.rowCount) {
                // Footer row
                classes.push('footer-cell');
                cell = column.footerValue == null ? '' : column.footerValue;
                title = column.footerTooltip == null ? '' : column.footerTooltip;
            } else {
                // Record row
                const result = this.props.record({ index: rowIndex - 1 });

                cell = column.cellRenderer(result);

                if (column.tooltip) {
                    title = column.tooltip(result);
                }
            }
        }

        if (typeof cell !== 'string') {
            classes.push('custom');
        }

        return (
            <div
                className={classes.join(' ')}
                key={`${columnIndex}, ${rowIndex}`}
                style={style}
                title={title}
            >
                {typeof cell === 'string' ? (
                    <span className="DataTable-cell-text">{cell}</span>
                ) : cell}
            </div>
        );
    }
}
