import React, { ReactNode } from "react";
import {
    GridCellRenderer,
    Index,
    MultiGrid,
    SortDirectionType,
    SortDirection,
} from "react-virtualized";
import "./BigTable.less";

export type Column<T> = {
    key?: string;
    name: string;
    width: number;
    cell_renderer: (record: T) => ReactNode;
    tooltip?: (record: T) => string;
    footer_value?: string;
    footer_tooltip?: string;
    /**
     * "number" and "integrated" have special meaning.
     */
    class_name?: string;
    sortable?: boolean;
};

export type ColumnSort<T> = { column: Column<T>; direction: SortDirectionType };

/**
 * A table with a fixed header. Optionally has fixed columns and a footer.
 * Uses windowing to support large amounts of rows and columns.
 * TODO: no-content message.
 */
export class BigTable<T> extends React.Component<{
    width: number;
    height: number;
    row_count: number;
    overscan_row_count?: number;
    columns: Array<Column<T>>;
    fixed_column_count?: number;
    overscan_column_count?: number;
    record: (index: Index) => T;
    footer?: boolean;
    /**
     * When this changes, the DataTable will re-render.
     */
    update_trigger?: any;
    sort?: (sort_columns: Array<ColumnSort<T>>) => void;
}> {
    private sort_columns = new Array<ColumnSort<T>>();

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
                    rowCount={this.props.row_count + 1 + (this.props.footer ? 1 : 0)}
                    fixedRowCount={1}
                    overscanRowCount={this.props.overscan_row_count}
                    columnWidth={this.column_width}
                    columnCount={this.props.columns.length}
                    fixedColumnCount={this.props.fixed_column_count}
                    overscanColumnCount={this.props.overscan_column_count}
                    cellRenderer={this.cell_renderer}
                    classNameTopLeftGrid="DataTable-header"
                    classNameTopRightGrid="DataTable-header"
                    updateTigger={this.props.update_trigger}
                />
            </div>
        );
    }

    private column_width = ({ index }: Index): number => {
        return this.props.columns[index].width;
    };

    private cell_renderer: GridCellRenderer = ({ columnIndex, rowIndex, style }) => {
        const column = this.props.columns[columnIndex];
        let cell: ReactNode;
        let sort_indicator: ReactNode;
        let title: string | undefined;
        const classes = ["DataTable-cell"];

        if (columnIndex === this.props.columns.length - 1) {
            classes.push("last-in-row");
        }

        if (rowIndex === 0) {
            // Header row
            cell = title = column.name;

            if (column.sortable) {
                classes.push("sortable");

                const sort = this.sort_columns[0];

                if (sort && sort.column === column) {
                    if (sort.direction === SortDirection.ASC) {
                        sort_indicator = (
                            <svg
                                className="DataTable-sort-indictator"
                                width="18"
                                height="18"
                                viewBox="0 0 24 24"
                            >
                                <path d="M7 14l5-5 5 5z"></path>
                                <path d="M0 0h24v24H0z" fill="none"></path>
                            </svg>
                        );
                    } else {
                        sort_indicator = (
                            <svg
                                className="DataTable-sort-indictator"
                                width="18"
                                height="18"
                                viewBox="0 0 24 24"
                            >
                                <path d="M7 10l5 5 5-5z"></path>
                                <path d="M0 0h24v24H0z" fill="none"></path>
                            </svg>
                        );
                    }
                }
            }
        } else {
            // Record or footer row
            if (column.class_name) {
                classes.push(column.class_name);
            }

            if (this.props.footer && rowIndex === 1 + this.props.row_count) {
                // Footer row
                classes.push("footer-cell");
                cell = column.footer_value == null ? "" : column.footer_value;
                title = column.footer_tooltip == null ? "" : column.footer_tooltip;
            } else {
                // Record row
                const result = this.props.record({ index: rowIndex - 1 });

                cell = column.cell_renderer(result);

                if (column.tooltip) {
                    title = column.tooltip(result);
                }
            }
        }

        if (typeof cell !== "string") {
            classes.push("custom");
        }

        const on_click =
            rowIndex === 0 && column.sortable ? () => this.header_clicked(column) : undefined;

        return (
            <div
                className={classes.join(" ")}
                key={`${columnIndex}, ${rowIndex}`}
                style={style}
                title={title}
                onClick={on_click}
            >
                {typeof cell === "string" ? (
                    <span className="DataTable-cell-text">{cell}</span>
                ) : (
                    cell
                )}
                {sort_indicator}
            </div>
        );
    };

    private header_clicked = (column: Column<T>) => {
        const old_index = this.sort_columns.findIndex(sc => sc.column === column);
        let old = old_index === -1 ? undefined : this.sort_columns.splice(old_index, 1)[0];

        const direction =
            old_index === 0 && old!.direction === SortDirection.ASC
                ? SortDirection.DESC
                : SortDirection.ASC;

        this.sort_columns.unshift({ column, direction });
        this.sort_columns.splice(10);

        if (this.props.sort) {
            this.props.sort(this.sort_columns);
        }
    };
}
