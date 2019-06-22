import React, { ReactNode } from "react";
import { GridCellRenderer, Index, MultiGrid, SortDirectionType, SortDirection } from "react-virtualized";
import "./BigTable.less";

export type Column<T> = {
    key?: string,
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
    sortable?: boolean
}

export type ColumnSort<T> = { column: Column<T>, direction: SortDirectionType }

/**
 * A table with a fixed header. Optionally has fixed columns and a footer.
 * Uses windowing to support large amounts of rows and columns.
 * TODO: no-content message.
 */
export class BigTable<T> extends React.Component<{
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
    updateTrigger?: any,
    sort?: (sortColumns: Array<ColumnSort<T>>) => void
}> {
    private sortColumns = new Array<ColumnSort<T>>();

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
        let sortIndicator: ReactNode;
        let title: string | undefined;
        const classes = ['DataTable-cell'];

        if (columnIndex === this.props.columns.length - 1) {
            classes.push('last-in-row');
        }

        if (rowIndex === 0) {
            // Header row
            cell = title = column.name;

            if (column.sortable) {
                classes.push('sortable');

                const sort = this.sortColumns[0];

                if (sort && sort.column === column) {
                    if (sort.direction === SortDirection.ASC) {
                        sortIndicator = (
                            <svg className="DataTable-sort-indictator" width="18" height="18" viewBox="0 0 24 24">
                                <path d="M7 14l5-5 5 5z"></path>
                                <path d="M0 0h24v24H0z" fill="none"></path>
                            </svg>
                        );
                    } else {
                        sortIndicator = (
                            <svg className="DataTable-sort-indictator" width="18" height="18" viewBox="0 0 24 24">
                                <path d="M7 10l5 5 5-5z"></path>
                                <path d="M0 0h24v24H0z" fill="none"></path>
                            </svg>
                        );
                    }
                }
            }
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

        const onClick = rowIndex === 0 && column.sortable
            ? () => this.headerClicked(column)
            : undefined;

        return (
            <div
                className={classes.join(' ')}
                key={`${columnIndex}, ${rowIndex}`}
                style={style}
                title={title}
                onClick={onClick}
            >
                {typeof cell === 'string' ? (
                    <span className="DataTable-cell-text">{cell}</span>
                ) : cell}
                {sortIndicator}
            </div>
        );
    }

    private headerClicked = (column: Column<T>) => {
        const oldIndex = this.sortColumns.findIndex(sc => sc.column === column);
        let old = oldIndex === -1 ? undefined : this.sortColumns.splice(oldIndex, 1)[0];

        const direction = oldIndex === 0 && old!.direction === SortDirection.ASC
            ? SortDirection.DESC
            : SortDirection.ASC

        this.sortColumns.unshift({ column, direction });
        this.sortColumns.splice(10);

        if (this.props.sort) {
            this.props.sort(this.sortColumns);
        }
    }
}
