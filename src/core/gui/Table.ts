import { Widget, WidgetOptions } from "./Widget";
import { el } from "./dom";
import {
    ListChangeType,
    ListProperty,
    ListPropertyChangeEvent,
} from "../observable/property/list/ListProperty";
import { Disposer } from "../observable/Disposer";
import "./Table.css";
import Logger = require("js-logger");

const logger = Logger.get("core/gui/Table");

export type Column<T> = {
    key?: string;
    title: string;
    fixed?: boolean;
    width: number;
    input?: boolean;
    text_align?: string;
    tooltip?: (value: T) => string;
    sortable?: boolean;
    render_cell(value: T, disposer: Disposer): string | HTMLElement;
    footer?: {
        render_cell(): string;
        tooltip?(): string;
    };
};

export enum SortDirection {
    Asc,
    Desc,
}

export type TableOptions<T> = WidgetOptions & {
    values: ListProperty<T>;
    columns: Column<T>[];
    sort?(sort_columns: { column: Column<T>; direction: SortDirection }[]): void;
};

export class Table<T> extends Widget<HTMLTableElement> {
    private readonly table_disposer = this.disposable(new Disposer());
    private readonly tbody_element = el.tbody();
    private readonly footer_row_element?: HTMLTableRowElement;
    private readonly values: ListProperty<T>;
    private readonly columns: Column<T>[];

    constructor(options: TableOptions<T>) {
        super(el.table({ class: "core_Table" }), options);

        this.values = options.values;
        this.columns = options.columns;

        let sort_columns: { column: Column<T>; direction: SortDirection }[] = [];

        const thead_element = el.thead();
        const header_tr_element = el.tr();

        let left = 0;
        let has_footer = false;

        header_tr_element.append(
            ...this.columns.map((column, index) => {
                const th = el.th(
                    { data: { index: index.toString() } },
                    el.span({ text: column.title }),
                );

                if (column.fixed) {
                    th.style.position = "sticky";
                    th.style.left = `${left}px`;
                    left += column.width;
                }

                th.style.width = `${column.width}px`;

                if (column.footer) {
                    has_footer = true;
                }

                return th;
            }),
        );

        const sort = options.sort;

        if (sort) {
            header_tr_element.onmousedown = e => {
                if (e.target instanceof HTMLElement) {
                    let element: HTMLElement = e.target;

                    for (let i = 0; i < 5; i++) {
                        if (element.dataset.index) {
                            break;
                        } else if (element.parentElement) {
                            element = element.parentElement;
                        } else {
                            return;
                        }
                    }

                    if (!element.dataset.index) return;

                    const index = parseInt(element.dataset.index, 10);
                    const column = this.columns[index];
                    if (!column.sortable) return;

                    const existing_index = sort_columns.findIndex(sc => sc.column === column);

                    if (existing_index === 0) {
                        const sc = sort_columns[0];
                        sc.direction =
                            sc.direction === SortDirection.Asc
                                ? SortDirection.Desc
                                : SortDirection.Asc;
                    } else {
                        if (existing_index !== -1) {
                            sort_columns.splice(existing_index, 1);
                        }

                        sort_columns.unshift({ column, direction: SortDirection.Asc });
                    }

                    sort(sort_columns);
                }
            };
        }

        thead_element.append(header_tr_element);
        this.tbody_element = el.tbody();
        this.element.append(thead_element, this.tbody_element);

        if (has_footer) {
            this.footer_row_element = el.tr();
            this.element.append(el.tfoot({}, this.footer_row_element));
            this.create_footer();
        }

        this.disposables(this.values.observe_list(this.update_table));

        this.splice_rows(0, this.values.length.val, this.values.val);

        this.finalize_construction(Table.prototype);
    }

    private update_table = (change: ListPropertyChangeEvent<T>): void => {
        if (change.type === ListChangeType.ListChange) {
            this.splice_rows(change.index, change.removed.length, change.inserted);
            this.update_footer();
        } else if (change.type === ListChangeType.ValueChange) {
            // TODO: update rows
        }
    };

    private splice_rows = (index: number, amount: number, inserted: T[]) => {
        for (let i = 0; i < amount; i++) {
            this.tbody_element.children[index].remove();
        }

        this.table_disposer.dispose_at(index, amount);

        const rows = inserted.map((value, i) => this.create_row(index + i, value));

        if (index >= this.tbody_element.childElementCount) {
            this.tbody_element.append(...rows);
        } else {
            for (let i = 0; i < amount; i++) {
                this.tbody_element.children[index + i].insertAdjacentElement(
                    "beforebegin",
                    rows[i],
                );
            }
        }
    };

    private create_row = (index: number, value: T): HTMLTableRowElement => {
        const disposer = this.table_disposer.add(new Disposer());
        let left = 0;

        return el.tr(
            {},
            ...this.columns.map((column, i) => {
                const cell = column.fixed ? el.th() : el.td();

                try {
                    const content = column.render_cell(value, disposer);

                    cell.append(content);

                    if (column.input) cell.classList.add("input");

                    if (column.fixed) {
                        cell.classList.add("fixed");
                        cell.style.left = `${left}px`;
                        left += column.width || 0;
                    }

                    cell.style.width = `${column.width}px`;

                    if (column.text_align) cell.style.textAlign = column.text_align;

                    if (column.tooltip) cell.title = column.tooltip(value);
                } catch (e) {
                    logger.warn(`Error while rendering cell for index ${index}, column ${i}.`, e);
                }

                return cell;
            }),
        );
    };

    private create_footer(): void {
        const footer_cells: HTMLTableHeaderCellElement[] = [];
        let left = 0;

        for (let i = 0; i < this.columns.length; i++) {
            const column = this.columns[i];
            const cell = el.th();

            cell.style.width = `${column.width}px`;

            if (column.fixed) {
                cell.classList.add("fixed");
                cell.style.left = `${left}px`;
                left += column.width || 0;
            }

            if (column.footer) {
                cell.textContent = column.footer.render_cell();
                cell.title = column.footer.tooltip ? column.footer.tooltip() : "";
            }

            if (column.text_align) cell.style.textAlign = column.text_align;

            footer_cells.push(cell);
        }

        this.footer_row_element!.append(...footer_cells);
    }

    private update_footer(): void {
        if (!this.footer_row_element) return;

        const col_count = this.columns.length;

        for (let i = 0; i < col_count; i++) {
            const column = this.columns[i];

            if (column.footer) {
                const cell = this.footer_row_element.children[i] as HTMLTableHeaderCellElement;
                cell.textContent = column.footer.render_cell();
                cell.title = column.footer.tooltip ? column.footer.tooltip() : "";
            }
        }
    }
}
