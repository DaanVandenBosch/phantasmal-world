import { Widget, WidgetOptions } from "./Widget";
import { el } from "./dom";
import {
    ListChangeType,
    ListProperty,
    ListPropertyChangeEvent,
} from "../observable/property/list/ListProperty";
import { Disposer } from "../observable/Disposer";
import "./Table.css";

export type Column<T> = {
    title: string;
    sticky?: boolean;
    width?: number;
    create_cell(value: T, disposer: Disposer): HTMLTableCellElement;
};

export type TableOptions<T> = WidgetOptions & {
    values: ListProperty<T>;
    columns: Column<T>[];
};

export class Table<T> extends Widget<HTMLTableElement> {
    private readonly table_disposer = this.disposable(new Disposer());
    private readonly tbody_element = el.tbody();
    private readonly values: ListProperty<T>;
    private readonly columns: Column<T>[];

    constructor(options: TableOptions<T>) {
        super(el.table({ class: "core_Table" }), options);

        this.values = options.values;
        this.columns = options.columns;

        const thead_element = el.thead();
        const header_tr_element = el.tr();

        let left = 0;

        header_tr_element.append(
            ...this.columns.map(column => {
                const th = el.th({
                    text: column.title,
                });

                if (column.width != undefined) th.style.width = `${column.width}px`;

                if (column.sticky) {
                    th.style.position = "sticky";
                    th.style.left = `${left}px`;
                    left += column.width || 0;
                }

                return th;
            }),
        );

        thead_element.append(header_tr_element);
        this.tbody_element = el.tbody();
        this.element.append(thead_element, this.tbody_element);

        this.disposables(this.values.observe_list(this.update_table));
    }

    private update_table = (change: ListPropertyChangeEvent<T>): void => {
        if (change.type === ListChangeType.ListChange) {
            this.splice_rows(change.index, change.removed.length, change.inserted);
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
                this.tbody_element.children[index + i].insertAdjacentElement("afterend", rows[i]);
            }
        }
    };

    private create_row = (index: number, value: T): HTMLTableRowElement => {
        const disposer = this.table_disposer.add(new Disposer());
        let left = 0;

        return el.tr(
            {},
            ...this.columns.map(column => {
                const cell = column.create_cell(value, disposer);

                if (column.width != undefined) cell.style.width = `${column.width}px`;

                if (column.sticky) {
                    cell.style.position = "sticky";
                    cell.style.left = `${left}px`;
                    left += column.width || 0;
                }

                return cell;
            }),
        );
    };
}
