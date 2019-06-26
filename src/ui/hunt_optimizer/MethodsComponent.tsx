import { TimePicker } from "antd";
import { observer } from "mobx-react";
import moment, { Moment } from "moment";
import React from "react";
import { AutoSizer, Index, SortDirection } from "react-virtualized";
import { Episode, HuntMethod } from "../../domain";
import { EnemyNpcTypes, NpcType } from "../../domain/NpcType";
import { huntMethodStore } from "../../stores/HuntMethodStore";
import { BigTable, Column, ColumnSort } from "../BigTable";
import "./MethodsComponent.css";

@observer
export class MethodsComponent extends React.Component {
    static columns: Array<Column<HuntMethod>> = (() => {
        // Standard columns.
        const columns: Column<HuntMethod>[] = [
            {
                key: 'name',
                name: 'Method',
                width: 250,
                cellRenderer: (method) => method.name,
                sortable: true,
            },
            {
                key: 'episode',
                name: 'Ep.',
                width: 34,
                cellRenderer: (method) => Episode[method.episode],
                sortable: true,
            },
            {
                key: 'time',
                name: 'Time',
                width: 50,
                cellRenderer: (method) => <TimeComponent method={method} />,
                className: 'integrated',
                sortable: true,
            },
        ];

        // One column per enemy type.
        for (const enemy of EnemyNpcTypes) {
            columns.push({
                key: enemy.code,
                name: enemy.name,
                width: 75,
                cellRenderer: (method) => {
                    const count = method.enemy_counts.get(enemy);
                    return count == null ? '' : count.toString();
                },
                className: 'number',
                sortable: true,
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
                        <BigTable<HuntMethod>
                            width={width}
                            height={height}
                            rowCount={methods.length}
                            columns={MethodsComponent.columns}
                            fixedColumnCount={3}
                            record={this.record}
                            sort={this.sort}
                            updateTrigger={huntMethodStore.methods.current.value}
                        />
                    )}
                </AutoSizer>
            </section>
        );
    }

    private record = ({ index }: Index) => {
        return huntMethodStore.methods.current.value[index];
    }

    private sort = (sorts: ColumnSort<HuntMethod>[]) => {
        const methods = huntMethodStore.methods.current.value.slice();

        methods.sort((a, b) => {
            for (const { column, direction } of sorts) {
                let cmp = 0;

                if (column.key === 'name') {
                    cmp = a.name.localeCompare(b.name);
                } else if (column.key === 'episode') {
                    cmp = a.episode - b.episode;
                } else if (column.key === 'time') {
                    cmp = a.time - b.time;
                } else if (column.key) {
                    const type = NpcType.by_code(column.key);

                    if (type) {
                        cmp = (a.enemy_counts.get(type) || 0) - (b.enemy_counts.get(type) || 0);
                    }
                }

                if (cmp !== 0) {
                    return direction === SortDirection.ASC ? cmp : -cmp;
                }
            }

            return 0;
        });

        huntMethodStore.methods.current.value = methods;
    }
}

@observer
class TimeComponent extends React.Component<{ method: HuntMethod }> {
    render() {
        const time = this.props.method.time;
        const hour = Math.floor(time);
        const minute = Math.round(60 * (time - hour));

        return (
            <TimePicker
                className="ho-MethodsComponent-timepicker"
                value={moment({ hour, minute })}
                format="HH:mm"
                size="small"
                allowClear={false}
                suffixIcon={<span />}
                onChange={this.change}
            />
        );
    }

    private change = (time: Moment) => {
        this.props.method.user_time = time.hour() + time.minute() / 60;
    }
}
